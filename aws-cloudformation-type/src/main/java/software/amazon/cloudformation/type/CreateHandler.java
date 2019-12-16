package software.amazon.cloudformation.type;

import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class CreateHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> request;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;

        this.proxy = proxy;
        this.request = request;
        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        if (!context.isCreateStarted()) {
            try {
                request.getDesiredResourceState().setTypeName(model.getTypeName());
                new ReadHandler().handleRequest(proxy, request, context, logger);
                throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME,
                    Objects.toString(model.getPrimaryIdentifier()));
            } catch (CfnNotFoundException e) {
                logger.log(request.getDesiredResourceState().getPrimaryIdentifier() +
                    " does not exist; creating the resource.");
            }

            registerType(proxy, model, context, logger);
        }

        return checkTypeRegistrationCompletion(proxy, model, context, logger);
    }

    RegisterTypeResponse registerType(
        final AmazonWebServicesClientProxy proxy,
        final ResourceModel model,
        final CallbackContext callbackContext,
        final Logger logger) {

        final RegisterTypeResponse response;

        try {
            response = proxy.injectCredentialsAndInvokeV2(Translator.translateToCreateRequest(model),
                ClientBuilder.getClient()::registerType);
            logger.log(String.format("%s registration successfully initiated [%s].",
                ResourceModel.TYPE_NAME, response.registrationToken()));
        } catch (final CfnRegistryException e) {
            throw new CfnGeneralServiceException(e);
        }

        callbackContext.setCreateStarted(true);
        callbackContext.setRegistrationToken(response.registrationToken());

        return response;
    }

    ProgressEvent<ResourceModel, CallbackContext> checkTypeRegistrationCompletion(
        final AmazonWebServicesClientProxy proxy,
        final ResourceModel model,
        final CallbackContext callbackContext,
        final Logger logger) {


        try {
            final DescribeTypeRegistrationRequest request = DescribeTypeRegistrationRequest.builder()
                .registrationToken(callbackContext.getRegistrationToken())
                .build();
            final DescribeTypeRegistrationResponse response = proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::describeTypeRegistration);

            if (response.progressStatus().equals(RegistrationStatus.COMPLETE)) {
                logger.log(String.format("%s registration successfully completed [%s].",
                    ResourceModel.TYPE_NAME, response.typeVersionArn()));

                model.setArn(response.typeVersionArn());
                model.setVersionId(response.typeVersionArn().substring(response.typeVersionArn().lastIndexOf('/') + 1));
            } else if (response.progressStatus().equals(RegistrationStatus.FAILED)) {
                throw new CfnGeneralServiceException(response.progressStatusAsString());
            } else {
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(callbackContext)
                    .callbackDelaySeconds(3)
                    .status(OperationStatus.IN_PROGRESS)
                    .resourceModel(model)
                    .build();
            }
        } catch (CfnNotFoundException e) {
            logger.log(request.getDesiredResourceState().getPrimaryIdentifier() +
                " does not exist; stabilization failed.");
            return ProgressEvent.defaultFailureHandler(e, HandlerErrorCode.NotStabilized);
        } catch (final CfnRegistryException e) {
            throw new CfnGeneralServiceException(e);
        }

        callbackContext.setCreateStabilized(true);

        return ProgressEvent.defaultSuccessHandler(model);
    }
}
