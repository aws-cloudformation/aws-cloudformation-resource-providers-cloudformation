package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.OperationOperator;
import software.amazon.cloudformation.stackset.util.ResourceModelBuilder;

public class ReadHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        final CloudFormationClient client = ClientBuilder.getClient();
        final OperationOperator operator = OperationOperator.builder().proxy(proxy).client(client).build();

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(ResourceModelBuilder.builder()
                        .proxy(proxy)
                        .client(client)
                        .stackSet(operator.getStackSet(model.getStackSetId()))
                        .build().buildModel())
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
