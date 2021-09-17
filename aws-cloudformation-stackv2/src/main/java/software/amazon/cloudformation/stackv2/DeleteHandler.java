package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
            .then(progress -> existenceCheck(progress, proxy, proxyClient, callbackContext, request, "AWS-CloudFormation-StackV2::Delete::PreDeletionCheck", logger))
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-StackV2::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToDeleteRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        DeleteStackResponse awsResponse = null;
                        try {
                            awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::deleteStack);
                        } catch (final AwsServiceException e) {
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        logger.log(String.format("%s successfully deleted.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> {
                        DescribeStacksResponse describeStacksResponse = null;
                        try {
                            describeStacksResponse = client.injectCredentialsAndInvokeV2(DescribeStacksRequest.builder()
                                .stackName(_model.getArn()).build(), proxyClient.client()::describeStacks);
                        } catch (final AwsServiceException e) {
                            if (e.getMessage().contains("does not exist")) {
                                logger.log(String.format("%s %s deletion has stabilized", ResourceModel.TYPE_NAME, _model.getPrimaryIdentifier()));
                                return true;
                            }
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        if (describeStacksResponse.stacks().isEmpty()) {
                            return true;
                        }
                        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
                            case DELETE_COMPLETE: return true;
                            case DELETE_FAILED: throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, _model.getArn());
                            default: return false;
                        }
                    })
                    .progress()
            )
            .then(progress -> ProgressEvent.defaultSuccessHandler(null));
    }
}
