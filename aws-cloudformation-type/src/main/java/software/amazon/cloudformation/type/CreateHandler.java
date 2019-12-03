package software.amazon.cloudformation.type;

import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.ResourceAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
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

            registerType(model, context);
        }

        if (!context.isCreateStabilized()) {
            try {
                final DescribeTypeRegistrationResponse response = checkTypeRegistrationCompletion(context);
                if (response.progressStatus().equals(RegistrationStatus.COMPLETE)) {
                    context.setCreateStabilized(true);
                } else if (response.progressStatus().equals(RegistrationStatus.FAILED)) {
                    throw new CfnGeneralServiceException(response.progressStatusAsString());
                }
            } catch (CfnNotFoundException e) {
                logger.log(request.getDesiredResourceState().getPrimaryIdentifier() +
                    " does not exist; retrying stabilization.");
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .callbackContext(context)
                    .callbackDelaySeconds(5)
                    .status(OperationStatus.IN_PROGRESS)
                    .resourceModel(request.getDesiredResourceState())
                    .build();
            }
        }

        final String createMessage = String.format("%s [%s] successfully created.",
            ResourceModel.TYPE_NAME, model.getTypeName());
        logger.log(createMessage);

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private void registerType(final ResourceModel model,
                              final CallbackContext callbackContext) {
        try {
            proxy.injectCredentialsAndInvokeV2(Translator.translateToCreateRequest(model),
                ClientBuilder.getClient()::registerType);
        } catch (final CfnRegistryException e) {
            throw new CfnGeneralServiceException(e);
        }
    }

    private DescribeTypeRegistrationResponse checkTypeRegistrationCompletion(final CallbackContext callbackContext) {
        final DescribeTypeRegistrationRequest request = DescribeTypeRegistrationRequest.builder()
            .registrationToken(callbackContext.getRegistrationToken())
            .build();

        try {
            return proxy.injectCredentialsAndInvokeV2(request,
                ClientBuilder.getClient()::describeTypeRegistration);
        } catch (final CfnRegistryException e) {
            throw new CfnGeneralServiceException(e);
        }
    }
}
