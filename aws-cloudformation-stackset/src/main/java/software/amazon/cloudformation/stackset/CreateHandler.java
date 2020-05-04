package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.StackInstancesPlaceHolder;
import software.amazon.cloudformation.stackset.util.Validator;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackSetRequest;

public class CreateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        // Ensure the idempotency of StackSet, we should not generate a random StackSetName
        final String stackSetName = request.getLogicalResourceIdentifier();
        final StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        analyzeTemplate(proxy, model, placeHolder);

        return proxy.initiate("AWS-CloudFormation-StackSet::Create", proxyClient, model, callbackContext)
                .translateToServiceRequest(resourceModel -> createStackSetRequest(resourceModel, stackSetName, request.getClientRequestToken()))
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final CreateStackSetResponse response = proxyClient.injectCredentialsAndInvokeV2(modelRequest, proxyClient.client()::createStackSet);
                    model.setStackSetId(response.stackSetId());
                    logger.log(String.format("%s [%s] StackSet creation succeeded", ResourceModel.TYPE_NAME, model.getStackSetId()));
                    return response;
                })
                .progress()
                .then(progress -> createStackInstances(proxy, proxyClient, progress, placeHolder.getCreateStackInstances(), logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Analyzes/validates template and StackInstancesGroup
     *
     * @param proxy       {@link AmazonWebServicesClientProxy}
     * @param model       {@link ResourceModel}
     * @param placeHolder {@link StackInstancesPlaceHolder}
     */
    private void analyzeTemplate(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final StackInstancesPlaceHolder placeHolder) {

        new Validator().validateTemplate(proxy, model.getTemplateBody(), model.getTemplateURL(), logger);
        InstancesAnalyzer.builder().desiredModel(model).build().analyzeForCreate(placeHolder);
    }
}
