package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.OperationOperator;
import software.amazon.cloudformation.stackset.util.ResourceModelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.listStackSetsRequest;

public class ListHandler extends BaseHandler<CallbackContext> {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        final CloudFormationClient client = ClientBuilder.getClient();
        final OperationOperator operator = OperationOperator.builder().proxy(proxy).client(client).build();

        final ListStackSetsResponse response = proxy.injectCredentialsAndInvokeV2(
                listStackSetsRequest(request.getNextToken()), client::listStackSets);

        final List<ResourceModel> models = response
                .summaries()
                .stream()
                .map(stackSetSummary -> ResourceModelBuilder.builder()
                        .proxy(proxy)
                        .client(client)
                        .stackSet(operator.getStackSet(stackSetSummary.stackSetId()))
                        .build().buildModel())
                .collect(Collectors.toList());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .build();
    }
}
