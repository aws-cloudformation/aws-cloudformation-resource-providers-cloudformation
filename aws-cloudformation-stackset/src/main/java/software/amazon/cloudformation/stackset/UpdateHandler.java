package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.Validator;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.updateStackSetRequest;

public class UpdateHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();
        analyzeTemplate(proxy, previousModel, model, callbackContext);

        return updateStackSet(proxy, proxyClient, model, callbackContext)
                .then(progress -> deleteStackInstances(proxy, proxyClient, progress, logger))
                .then(progress -> createStackInstances(proxy, proxyClient, progress, logger))
                .then(progress -> updateStackInstances(proxy, proxyClient, progress, logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param proxy {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client the aws service client {@link ProxyClient<CloudFormationClient>} to make the call
     * @param model {@link ResourceModel}
     * @param callbackContext {@link CallbackContext}
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    protected ProgressEvent<ResourceModel, CallbackContext> updateStackSet(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ResourceModel model,
            final CallbackContext callbackContext) {

        return proxy.initiate("AWS-CloudFormation-StackSet::UpdateStackSet", client, model, callbackContext)
                .request(modelRequest -> updateStackSetRequest(modelRequest))
                .retry(MULTIPLE_OF)
                .call((modelRequest, proxyInvocation) ->
                        proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::updateStackSet))
                .stabilize((request, response, proxyInvocation, resourceModel, context) ->
                        isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                .exceptFilter(this::filterException)
                .progress();
    }

    /**
     * Analyzes/validates template and StackInstancesGroup
     * @param proxy {@link AmazonWebServicesClientProxy}
     * @param previousModel previous {@link ResourceModel}
     * @param model {@link ResourceModel}
     * @param context {@link CallbackContext}
     */
    private void analyzeTemplate(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel previousModel,
            final ResourceModel model,
            final CallbackContext context) {

        new Validator().validateTemplate(proxy, model.getTemplateBody(), model.getTemplateURL(), logger);
        InstancesAnalyzer.builder().desiredModel(model).previousModel(previousModel).build().analyzeForUpdate(context);
    }
}
