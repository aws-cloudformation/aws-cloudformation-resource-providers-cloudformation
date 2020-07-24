package software.amazon.cloudformation.resourcedefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();

        // Delete is a no-op for this resource type as a default version must always exist for a type

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
