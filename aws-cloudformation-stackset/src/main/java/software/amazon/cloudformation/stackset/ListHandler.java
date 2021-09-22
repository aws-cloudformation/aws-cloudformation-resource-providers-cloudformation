package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.ResourceModelBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.listStackSetsRequest;

public class ListHandler extends BaseHandlerStd {

    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ListStackSetsResponse response = proxyClient.injectCredentialsAndInvokeV2(
                listStackSetsRequest(request.getNextToken()), proxyClient.client()::listStackSets);

        final List<ResourceModel> models = response
                .summaries()
                .stream()
                .map(stackSetSummary -> ResourceModelBuilder.builder()
                        .proxyClient(proxyClient)
                        .stackSet(describeStackSet(proxyClient, stackSetSummary.stackSetId(), logger))
                        .build()
                        .buildModel())
                .collect(Collectors.toList());

        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModels(models)
                .status(OperationStatus.SUCCESS)
                .nextToken(response.nextToken())
                .build();
    }
}
