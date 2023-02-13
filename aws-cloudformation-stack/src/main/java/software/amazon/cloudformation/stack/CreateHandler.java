package software.amazon.cloudformation.stack;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


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

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
                .then(progress -> {
                    if (model.getStackName() == null) {
                        model.setStackName(IdentifierUtils.generateResourceIdentifier("stack",
                        Optional.ofNullable(request.getClientRequestToken()).orElse("token"), STACK_NAME_MAX_LENGTH));
                    }
                return progress;
            })
            .then(progress -> {
                if (request.getDesiredResourceTags() != null && request.getDesiredResourceTags().size() != 0) {
                    List<Tag> resourceTags = request.getDesiredResourceTags().entrySet()
                        .stream()
                        .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                        .collect(Collectors.toList());
                  //  model.getTags().addAll(resourceTags);
                    if(model.getTags()  != null) {
                        resourceTags.addAll(model.getTags());
                    }
                    model.setTags(resourceTags);
                }
                return progress;
            })
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-Stack::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        CreateStackResponse awsResponse;
                        try {
                            logger.log(String.format("%s %s", this.getClass(), Optional.ofNullable(request.getClientRequestToken()).orElse("")));
                            awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, client.client()::createStack);
                        } catch (final AwsServiceException e) {
                            /*
                            * While the handler contract states that the handler must always return a progress event,
                            * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
                            * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
                            * to more specific error codes
                            */
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }

                        logger.log(String.format("%s successfully created.", ResourceModel.TYPE_NAME));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> {
                        model.setStackId(awsResponse.stackId());
                        DescribeStacksResponse describeStacksResponse;
                        try {
                            describeStacksResponse = client.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(_model), client.client()::describeStacks);
                        } catch (final AwsServiceException e) {
                            if (e.getMessage().contains("does not exist")) {
                                return false;
                            }
                            throw new CfnGeneralServiceException(e.getMessage(), e);
                        }
                        if (describeStacksResponse.stacks().isEmpty()) {
                            return false;
                        }
                        String stackId = _model.getStackId();
                        switch(describeStacksResponse.stacks().get(0).stackStatus()) {
                            case CREATE_COMPLETE: {
                                logger.log(String.format("%s [%s] has been stabilized.", ResourceModel.TYPE_NAME, _model.getPrimaryIdentifier()));
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
                    })
                    .progress()
                )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
