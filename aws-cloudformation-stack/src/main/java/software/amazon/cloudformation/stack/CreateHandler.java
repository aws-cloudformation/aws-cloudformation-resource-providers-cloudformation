package software.amazon.cloudformation.stack;

import com.google.common.collect.Maps;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;
        ResourceModel model = request.getDesiredResourceState();

        Map<String, String> mergedTags = Maps.newHashMap();
        mergedTags.putAll(Optional.ofNullable(request.getDesiredResourceTags()).orElse(Collections.emptyMap()));
        mergedTags.putAll(Optional.ofNullable(request.getSystemTags()).orElse(Collections.emptyMap()));

        //only chain this if stackName is null;
        if (model != null && model.getStackName() == null) {
            model.setStackName(IdentifierUtils.generateResourceIdentifier("stack",
                Optional.ofNullable(request.getClientRequestToken()).orElse("token"), STACK_NAME_MAX_LENGTH));
        }
        //chain this if mergedTags is not empty
        if (mergedTags != null && mergedTags.size() != 0) {
            List<Tag> resourceTags = Translator.mergeRequestTagWithModelTags(mergedTags, model);
            model.setTags(resourceTags);
        }
        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext).then(progress ->
                proxy.initiate("AWS-CloudFormation-Stack::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        logger.log(String.format("%s %s", this.getClass(), Optional.ofNullable(request.getClientRequestToken()).orElse("")));
                        CreateStackResponse awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::createStack);
                        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> stabilizeCreate(client, awsResponse,_model, logger))
                    .handleError((awsRequest, exception, client, _model, context) -> handleError(awsRequest, exception, client, _model, context))
                    .progress()
                )
            .then(progress -> ProgressEvent.defaultSuccessHandler(progress.getResourceModel()));
    }

    private boolean stabilizeCreate(ProxyClient<CloudFormationClient> proxyClient, CreateStackResponse awsResponse, ResourceModel model, Logger logger) {
        model.setStackId(awsResponse.stackId());
        DescribeStacksResponse describeStacksResponse;
        try {
            describeStacksResponse = proxyClient.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model), proxyClient.client()::describeStacks);
        } catch (final AwsServiceException e) {
            if (e.getMessage().contains("does not exist")) {
                return false;
            }
            throw e;
        }
        if (describeStacksResponse.stacks().isEmpty()) {
            return false;
        }
        String stackId = model.getStackId();
        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
            case CREATE_COMPLETE: {
                logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
                return true;
            }
            case CREATE_FAILED:
            case ROLLBACK_FAILED:
            case DELETE_COMPLETE:
            case DELETE_FAILED:
            case ROLLBACK_COMPLETE:
                throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, stackId);
            default: return false;
        }
    }

}
