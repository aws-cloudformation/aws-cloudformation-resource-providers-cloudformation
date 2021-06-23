package software.amazon.cloudformation.resourcedefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd{
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(AmazonWebServicesClientProxy proxy,
                                                                          ResourceHandlerRequest<ResourceModel> request,
                                                                          CallbackContext callbackContext,
                                                                          ProxyClient<CloudFormationClient> proxyClient,
                                                                          Logger logger) {
        final ResourceModel resourceModel = request.getDesiredResourceState() == null
                ? ResourceModel.builder().build()
                : request.getDesiredResourceState();
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
                proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        logger.log(String.format("List the resource default version with the identifier %s", resourceModel.getArn()));
        return initiator
                .translateToServiceRequest((model) -> Translator.translateToListRequest(resourceModel, request.getNextToken()))
                .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::listTypeVersions))
                .done((listTypesRequest, listTypesResponse, sdkProxyClient, model, cc) ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.SUCCESS)
                                .resourceModels(Translator.translateFromListResponse(listTypesResponse))
                                .nextToken(listTypesResponse.nextToken())
                                .build()
                );
    }
}
