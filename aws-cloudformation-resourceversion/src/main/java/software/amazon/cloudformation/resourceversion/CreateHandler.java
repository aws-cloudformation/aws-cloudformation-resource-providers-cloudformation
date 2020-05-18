package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRegistrationResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.RegisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.RegistrationStatus;
import software.amazon.awssdk.services.cloudformation.model.RegistryType;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.awssdk.services.cloudformation.model.TypeVersionSummary;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

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
            registerType(proxy, model, context, logger);
        }

        if (!context.isArnPredicted()) {
            return predictArn(proxy, request, context, logger);
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

    ProgressEvent<ResourceModel, CallbackContext> predictArn(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        String arn;
        try {
            // find the highest existing version number for this type
            while (true) {
                final ListTypeVersionsRequest listTypeVersionsRequest = ListTypeVersionsRequest.builder()
                    .type(RegistryType.RESOURCE)
                    .typeName(request.getDesiredResourceState().getTypeName())
                    .build();
                final ListTypeVersionsResponse response = proxy.injectCredentialsAndInvokeV2(listTypeVersionsRequest,
                    ClientBuilder.getClient()::listTypeVersions);

                if (response.nextToken() == null) {
                    // bump the version number for this revision
                    final TypeVersionSummary mostRecentVersion = response.typeVersionSummaries().get(response.typeVersionSummaries().size() - 1);
                    arn = mostRecentVersion.arn();
                    arn = arn
                        .substring(0, arn.lastIndexOf("/") + 1)
                        .concat(String.format("%08d", Integer.parseInt(mostRecentVersion.versionId()) + 1));
                    break;
                }
            }
        } catch (final TypeNotFoundException e) {
            // registration can be assumed to be the first version for a type
            arn = String.format("arn:{%s}:cloudformation:{%s}:{%s}:type/resource/{%s}/00000001",
                request.getAwsPartition(),
                request.getRegion(),
                request.getAwsAccountId(),
                request.getDesiredResourceState().getTypeName().replace("::", "-"));
        } catch (final CfnRegistryException e) {
            logger.log(Arrays.toString(e.getStackTrace()));
            throw new CfnGeneralServiceException(e);
        }

        callbackContext.setArnPredicted(true);
        request.getDesiredResourceState().setArn(arn);

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .callbackContext(callbackContext)
            .callbackDelaySeconds(3)
            .status(OperationStatus.IN_PROGRESS)
            .resourceModel(request.getDesiredResourceState())
            .build();
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
                logger.log(String.format("Registration request %s failed with '%s'", callbackContext.getRegistrationToken(), response.description()));
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getArn());
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
