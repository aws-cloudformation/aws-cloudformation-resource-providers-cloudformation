package software.amazon.cloudformation.stack;

import com.google.common.collect.Maps;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        logger.log(String.format("[StackId: %s, ClientRequestToken: %s] Calling Update VPN Gateway", request.getStackId(), request.getClientRequestToken()));

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
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private boolean stabilizeUpdate(ProxyClient<CloudFormationClient> proxyClient, Object awsResponse, ResourceModel model, Logger logger) {
        DescribeStacksResponse describeStacksResponse = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeStacks);
        if (describeStacksResponse.stacks().isEmpty()) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getStackId());
        }
        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
            case CREATE_COMPLETE:
            case UPDATE_COMPLETE:
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
