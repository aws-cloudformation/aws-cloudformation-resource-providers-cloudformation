package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();

        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
            proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        // pre-read to capture required metadata fields in model for Delete
        return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
            // now deregister the type
            .onSuccess(progress ->
                initiator.initiate("delete")
                    .translateToServiceRequest(model ->
                        Translator.translateToDeleteRequest(progress.getResourceModel(), logger))
                    .makeServiceCall((awsRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(awsRequest, proxyInvocation.client()::deregisterType))
                    .success());
    }
}
