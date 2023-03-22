package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateTerminationProtectionResponse;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;


public class UpdateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        ResourceModel model = request.getDesiredResourceState();
        ResourceModel previousModel = request.getPreviousResourceState();

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Update Stack", request.getStackId(), request.getClientRequestToken()));
        if(model.getEnableTerminationProtection() != null && !model.getEnableTerminationProtection().equals(previousModel.getEnableTerminationProtection()))
            ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-Stack::UpdateTerminationProtection", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToUpdateTerminationProtectionRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        UpdateTerminationProtectionResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::updateTerminationProtection);
                        logger.log(String.format("TerminationProtection has successfully been updated for stack %s.", progress.getResourceModel().getStackId()));
                        return awsResponse;
                    })
                    .handleError((awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
                    .progress());
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-Stack::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToUpdateRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        UpdateStackResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::updateStack);
                        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> stabilizeUpdate(client, awsResponse,_model, logger))
                    .handleError((awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
                    .progress())

            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private boolean stabilizeUpdate(ProxyClient<CloudFormationClient> proxyClient, Object awsResponse, ResourceModel model, Logger logger) {
        DescribeStacksResponse describeStacksResponse = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeStacks);
        if (describeStacksResponse.stacks().isEmpty()) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getStackId());
        }
        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
            case CREATE_COMPLETE:
            case UPDATE_COMPLETE:
                //We will assume UPDATE_COMPLETE_CLEANUP_IN_PROGRESS is stabliazed status for now, this will unblock customer if the stack's update is actually done. But there is still some risk that resources have not been removed.
            case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS: return true;
            case UPDATE_ROLLBACK_COMPLETE:
            case UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS:
            case UPDATE_ROLLBACK_FAILED:
            case UPDATE_FAILED:
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getStackId());
            default: {
                return false;
            }
        }
    }
}
