package software.amazon.cloudformation.stackset;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackSetResponse;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.stackset.util.StackInstancesPlaceHolder;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackSetRequest;

public class DeleteHandler extends BaseHandlerStd {

    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        this.logger = logger;
        final ResourceModel model = request.getDesiredResourceState();
        // Analyzes stack instances group for delete
        final StackInstancesPlaceHolder placeHolder = new StackInstancesPlaceHolder();
        analyzeTemplate(proxyClient, request, placeHolder, Action.DELETE);

        return ProgressEvent.progress(model, callbackContext)
                // delete/stabilize progress chain - delete all associated stack instances
                .then(progress -> deleteStackInstances(proxy, proxyClient, progress, placeHolder.getDeleteStackInstances(), logger))
                .then(progress -> deleteStackSet(proxy, proxyClient, progress));
    }

    /**
     * Implement client invocation of the delete request through the proxyClient, which is already initialised with
     * caller credentials, correct region and retry settings
     *
     * @param proxy    Amazon webservice proxy to inject credentials correctly.
     * @param client   the aws service client to make the call
     * @param progress event of the previous state indicating success, in progress with delay callback or failed state
     * @return delete resource response
     */
    protected ProgressEvent<ResourceModel, CallbackContext> deleteStackSet(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        return proxy.initiate("AWS-CloudFormation-StackSet::DeleteStackSet", client, model, callbackContext)
                .translateToServiceRequest(modelRequest -> deleteStackSetRequest(modelRequest.getStackSetId()))
                .makeServiceCall((modelRequest, proxyInvocation) -> {
                    final DeleteStackSetResponse response = proxyInvocation.injectCredentialsAndInvokeV2(
                            deleteStackSetRequest(model.getStackSetId()), proxyInvocation.client()::deleteStackSet);
                    logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
                    return response;
                })
                .success();
    }
}
