package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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

        if (request.getDesiredResourceTags() != null) {
            model.setTags(request.getDesiredResourceTags().entrySet().stream()
                .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList()));
        }

        return ProgressEvent.progress(model, callbackContext)
            .then(progress -> existenceCheck(progress, proxy, proxyClient, callbackContext, request, "AWS-CloudFormation-StackV2::Update::PreUpdateCheck", logger))
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-StackV2::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
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
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> {
                        DescribeStacksResponse describeStacksResponse = null;
                        try {
                            describeStacksResponse = client.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(_model), proxyClient.client()::describeStacks);
                        } catch (final AwsServiceException e) {
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        if (describeStacksResponse.stacks().isEmpty()) {
                            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, _model.getArn());
                        }
                        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
                            case UPDATE_COMPLETE: return true;
                            case UPDATE_COMPLETE_CLEANUP_IN_PROGRESS: return true;
                            case UPDATE_ROLLBACK_COMPLETE: return true;
                            case UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS: return true;
                            case UPDATE_ROLLBACK_FAILED: throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, _model.getArn());
                            default: return false;
                        }
                    })
                    .progress())
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
