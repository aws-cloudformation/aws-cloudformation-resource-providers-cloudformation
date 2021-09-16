package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.services.cloudformation.model.ListStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStacksResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.awssdk.services.cloudformation.model.StackStatus.DELETE_COMPLETE;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ListStacksRequest awsRequest = Translator.translateToListRequest(request.getNextToken());
        final ListStacksResponse awsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, ClientBuilder.getClient()::listStacks);
        final String nextToken = awsResponse.nextToken();
        final List<ResourceModel> models = awsResponse.stackSummaries().stream()
            .filter(ss -> ss.stackStatus() != DELETE_COMPLETE)
            .map(ss -> ResourceModel.builder().arn(ss.stackId()).build())
            .collect(Collectors.toList());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(nextToken)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
