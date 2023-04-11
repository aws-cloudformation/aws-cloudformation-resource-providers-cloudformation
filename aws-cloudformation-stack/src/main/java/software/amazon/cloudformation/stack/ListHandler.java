package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
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
       logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling List Stack", request.getStackId(),
            request.getClientRequestToken()));
       return proxy.initiate("AWS-CloudFormation-Stack::List", proxyClient, request.getDesiredResourceState(), callbackContext)
           .translateToServiceRequest((rm) -> {
               String token = request.getNextToken();
               return Translator.translateToListRequest(token);
           })
           .makeServiceCall((awsRequest, client) -> client.injectCredentialsAndInvokeV2(awsRequest, client.client()::listStacks))
           .handleError((awsRequest, exception, client, model, context) -> handleError(awsRequest, exception, client, model, context))
           .done((rq, rp, client, model, context) ->  ProgressEvent.<ResourceModel, CallbackContext>builder()
               .resourceModels(Translator.translateFromListResponse(rp))
               .nextToken(rp.nextToken())
               .status(OperationStatus.SUCCESS)
               .build());
    }
}
