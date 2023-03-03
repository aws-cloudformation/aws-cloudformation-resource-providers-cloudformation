package software.amazon.cloudformation.stack;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Optional;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            // STEP 1 [check if resource already exists]
            // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
            // if target API does not support 'ResourceNotFoundException' then following check is required
            .then(progress -> existenceCheck(progress,proxy, proxyClient, callbackContext, request, "AWS-CloudFormation-Stack::Delete::PreDeletionCheck", logger))
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-Stack::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        logger.log(String.format("%s %s", this.getClass(), Optional.ofNullable(request.getClientRequestToken()).orElse("")));
                        DeleteStackResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::deleteStack);
                        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    // for more information -> https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
                    .stabilize((awsRequest, awsResponse, client, model, context) -> stabilizeDelete(proxyClient, awsResponse, model, logger))
                    .handleError((awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }

    private boolean stabilizeDelete(ProxyClient<CloudFormationClient> proxyClient, DeleteStackResponse awsResponse, ResourceModel model, Logger logger) {
        DescribeStacksResponse describeStacksResponse;
        try {
            describeStacksResponse = proxyClient.injectCredentialsAndInvokeV2(DescribeStacksRequest.builder()
                .stackName(model.getStackId()).build(), proxyClient.client()::describeStacks);
        } catch (final AwsServiceException e) {
            if (e.getMessage().contains("does not exist")) {
                logger.log(String.format("%s %s deletion has stabilized", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
                return true;
            }
            throw new CfnGeneralServiceException(e.getMessage(), e);
        }
        if (describeStacksResponse.stacks().isEmpty()) {
            return true;
        }
        StackStatus status = describeStacksResponse.stacks().get(0).stackStatus();
        switch(status) {
            case DELETE_COMPLETE: {
                logger.log(String.format("%s [%s] deletion has stabilized", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
                return true;
            }
            case DELETE_FAILED: throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getStackId());
            default: {
                logger.log(String.format("%s [%s] deletion is still in %s ", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier(), status));
                return false;
            }
        }
    }

}
