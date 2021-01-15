package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState() == null
                ? ResourceModel.builder().build()
                : request.getDesiredResourceState();
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
                proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        return initiator
                .translateToServiceRequest((model) -> Translator.translateToListRequest(request.getNextToken()))
                .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::listTypes))
                .done((listTypesRequest, listTypesResponse, sdkProxyClient, model, cc) ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.SUCCESS)
                                .resourceModels(Translator.translateFromListResponse(listTypesResponse))
                                .nextToken(listTypesResponse.nextToken())
                                .build()
                );
    }
}
