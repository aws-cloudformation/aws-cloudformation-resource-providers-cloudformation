package software.amazon.cloudformation.moduledefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class CreateHandler extends BaseHandlerStd {

    private ReadHandler readHandler;

    public CreateHandler() {
        this(new ReadHandler());
    }

    CreateHandler(final ReadHandler readHandler) {
        this.readHandler = readHandler;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        validateModel(model);

        if (defaultVersionExists(proxy, request, callbackContext, proxyClient)) {
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        }

        return proxy.initiate("AWS-CloudFormation-ModuleDefaultVersion::Create", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToCreateRequest)
                .makeServiceCall((request1, proxyClient1) -> setModuleDefaultVersion(request1, proxyClient, model))
                .done(progress -> model.getArn() == null // read is only required if Arn is not present
                        ? readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger)
                        : ProgressEvent.defaultSuccessHandler(model));
    }

    private boolean defaultVersionExists(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient) {

        ProgressEvent<ResourceModel, CallbackContext> response;
        try {
            response = readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger);
        } catch (final CfnNotFoundException exception) {
            return false;
        }

        if (response.isFailed()) {
            if (response.getErrorCode() != null && response.getErrorCode() != HandlerErrorCode.NotFound) {
                logger.log(String.format("Unexpected error code while checking if module default version exists: %s", response.getErrorCode()));
                throw new CfnGeneralServiceException("module default version existence check");
            }
            return false; // attempt to set default version on a failed read
        }

        return true;
    }

    private SetTypeDefaultVersionResponse setModuleDefaultVersion(
            final SetTypeDefaultVersionRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model) {

        SetTypeDefaultVersionResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::setTypeDefaultVersion);
        } catch (final TypeNotFoundException exception) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, getModelIdentifier(model));
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to set module as default version in registry:\n%s", Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }

    private String getModelIdentifier(final ResourceModel model) {
        return model.getPrimaryIdentifier() == null
                ? String.format("%s v%s", model.getModuleName(), model.getVersionId())
                : model.getPrimaryIdentifier().toString();
    }
}
