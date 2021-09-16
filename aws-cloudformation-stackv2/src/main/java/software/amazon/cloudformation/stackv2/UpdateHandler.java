package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
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

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-StackV2::Update::PreUpdateCheck", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToReadRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        if (request.getDesiredResourceState().getArn() == null && request.getDesiredResourceState().getStackName() == null) {
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getArn());
                        }
                        DescribeStacksResponse awsResponse = null;
                        try {
                            awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::describeStacks);
                        } catch (final AwsServiceException e) {
                            /*
                             * While the handler contract states that the handler must always return a progress event,
                             * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
                             * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
                             * to more specific error codes
                             */
                            if (e.getMessage().contains("does not exist")) {
                                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, e.getMessage());
                            }
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        if (awsResponse.stacks().isEmpty() || awsResponse.stacks().get(0).stackStatus() == StackStatus.DELETE_COMPLETE) {
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getArn());
                        }
                        logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    .progress()
            )
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-StackV2::Update::first", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToUpdateRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        UpdateStackResponse awsResponse = null;
                        try {
                            awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::updateStack);
                        } catch (final AwsServiceException e) {
                            /*
                            * While the handler contract states that the handler must always return a progress event,
                            * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
                            * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
                            * to more specific error codes
                            */
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        logger.log(String.format("%s has successfully been updated.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, model, context) -> {
                        DescribeStacksResponse describeStacksResponse = null;
                        try {
                            describeStacksResponse = client.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeStacks);
                        } catch (final AwsServiceException e) {
                            if (e.getMessage().contains("does not exist")) {
                                return false;
                            }
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        if (describeStacksResponse.stacks().isEmpty()) {
                            return false;
                        }
                        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
                            case UPDATE_COMPLETE: return true;
                            case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS: return true;
                            case UPDATE_ROLLBACK_COMPLETE: return true;
                            case UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS: return true;
                            case UPDATE_ROLLBACK_FAILED: throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, model.getArn());
                            default: return false;
                        }
                    })
                    .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
