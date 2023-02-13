package software.amazon.cloudformation.stack;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
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
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)

            // STEP 1 [check if resource already exists]
            // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            // if target API does not support 'ResourceNotFoundException' then following check is required
            .then(progress -> existenceCheck(progress,proxy, proxyClient, callbackContext, request,"AWS-CloudFormation-Stack::Update::PreUpdateCheck",logger))
            // STEP 2 [first update/stabilize progress chain - required for resource update]
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-Stack::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToUpdateRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        UpdateStackResponse awsResponse;
                        try {
                            awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::updateStack);
                        } catch (final AwsServiceException e) {
                            if (e.getMessage().contains("No updates are to be performed")) {
                                return ProgressEvent.defaultSuccessHandler(model);
                            }
                            throw new CfnGeneralServiceException(e.getMessage(), e);
                        }

                        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })

                    // stabilization step may or may not be needed after each API call
                    // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> {
                        DescribeStacksResponse describeStacksResponse = null;
                        try {
                            describeStacksResponse = client.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(_model), client.client()::describeStacks);
                        } catch (final AwsServiceException e) {
                            throw new CfnGeneralServiceException(e.getMessage(), e);
                        }
                        if (describeStacksResponse.stacks().isEmpty()) {
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, _model.getStackId());
                        }
                        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
                            case CREATE_COMPLETE:
                            case UPDATE_COMPLETE:
                            case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS: return true;
                            case UPDATE_ROLLBACK_COMPLETE:
                            case UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS:
                            case UPDATE_ROLLBACK_FAILED:
                            case UPDATE_FAILED:
                                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, _model.getStackId());
                            default: {
                                return false;
                            }
                        }
                    })
                    .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
