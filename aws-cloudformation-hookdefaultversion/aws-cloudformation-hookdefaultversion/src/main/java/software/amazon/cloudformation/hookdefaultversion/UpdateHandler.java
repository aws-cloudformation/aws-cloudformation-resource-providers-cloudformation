package software.amazon.cloudformation.hookdefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class UpdateHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
                proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        logger.log(String.format("Updating [TypeVersionArn: %s | Type: %s | Version: %s]",
                resourceModel.getTypeVersionArn(), resourceModel.getTypeName(), resourceModel.getVersionId()));

        return proxy.initiate("AWS-CloudFormation-HookDefaultVersion::Update", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .makeServiceCall((setTypeDefaultVersionRequest, client) -> proxyClient.injectCredentialsAndInvokeV2(setTypeDefaultVersionRequest, proxyClient.client()::setTypeDefaultVersion))
                .handleError((setTypeDefaultVersionRequest, exception, clientProxy, model, context) -> {
                    if (exception instanceof TypeNotFoundException) {
                        logger.log(String.format("Failed to set the default version of the hook [%s] as it cannot be found %s", model.getArn(), Arrays.toString(exception.getStackTrace())));
                        throw new CfnNotFoundException(exception);
                    } else {
                        logger.log(String.format("Failed to set the default version of the hook [%s] and the exception is [%s]", model.getArn(), Arrays.toString(exception.getStackTrace())));
                        throw new CfnGeneralServiceException(exception);
                    }
                })
                .done(setTypeDefaultVersionResponse -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
