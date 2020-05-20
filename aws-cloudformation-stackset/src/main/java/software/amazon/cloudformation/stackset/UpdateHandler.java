package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetResponse;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.StackInstancesPlaceHolder;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.updateStackSetRequest;
import static software.amazon.cloudformation.stackset.util.Comparator.isStackSetConfigEquals;

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
        final StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        analyzeTemplate(proxyClient, request, placeHolder, Action.UPDATE);

        return ProgressEvent.progress(model, callbackContext)
                .then(progress -> updateStackSet(proxy, proxyClient, progress, previousModel))
                .then(progress -> deleteStackInstances(proxy, proxyClient, progress, placeHolder.getDeleteStackInstances(), logger))
                .then(progress -> createStackInstances(proxy, proxyClient, progress, placeHolder.getCreateStackInstances(), logger))
                .then(progress -> updateStackInstances(proxy, proxyClient, progress, placeHolder.getUpdateStackInstances(), logger))
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    /**
     * Implement client invocation of the update request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param proxy         {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client        the aws service client {@link ProxyClient<CloudFormationClient>} to make the call
     * @param progress      {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @param previousModel previous {@link ResourceModel} for comparing with desired model
     * @return progressEvent indicating success, in progress with delay callback or failed state
     */
    private ProgressEvent<ResourceModel, CallbackContext> updateStackSet(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final ResourceModel previousModel) {

        final ResourceModel desiredModel = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        if (isStackSetConfigEquals(previousModel, desiredModel)) {
            return ProgressEvent.progress(desiredModel, callbackContext);
        }
        return proxy.initiate("AWS-CloudFormation-StackSet::UpdateStackSet", client, desiredModel, callbackContext)
                .translateToServiceRequest(modelRequest -> updateStackSetRequest(modelRequest))
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final UpdateStackSetResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::updateStackSet);
                    logger.log(String.format("%s UpdateStackSet initiated", ResourceModel.TYPE_NAME));
                    return response;
                })
                .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                .retryErrorFilter(this::filterException)
                .progress();
    }
}
