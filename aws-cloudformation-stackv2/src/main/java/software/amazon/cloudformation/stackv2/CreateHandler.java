package software.amazon.cloudformation.stackv2;

import org.apache.commons.lang3.RandomStringUtils;
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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

        if (model.getStackName() == null) {
            model.setStackName(RandomStringUtils.randomAlphabetic(STACK_NAME_MAX_LENGTH));
        }

        if (request.getSystemTags() != null) {
            final List<Tag> systemTags = request.getSystemTags().entrySet().stream()
                .map(e -> Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
            model.setTags(Stream.of(model.getTags(), systemTags).flatMap(Collection::stream).collect(Collectors.toList()));
        }

        return ProgressEvent.progress(model, callbackContext)
            .then(progress ->
                proxy.initiate("AWS-CloudFormation-StackV2::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                    .translateToServiceRequest(Translator::translateToCreateRequest)
                    .makeServiceCall((awsRequest, client) -> {
                        CreateStackResponse awsResponse = null;
                        try {
                            awsResponse = client.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::createStack);
                            logger.log(awsResponse.toString());
                            progress.getResourceModel().setArn(awsResponse.stackId());
                        } catch (final AwsServiceException e) {
                            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e);
                        }
                        logger.log(String.format("%s %s successfully created.", ResourceModel.TYPE_NAME, awsResponse.stackId()));
                        return awsResponse;
                    })
                    .stabilize((awsRequest, awsResponse, client, _model, context) -> {
                        DescribeStacksResponse describeStacksResponse = null;
                        try {
                            describeStacksResponse = client.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(_model), proxyClient.client()::describeStacks);
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
                            case CREATE_COMPLETE: return true;
                            case CREATE_FAILED: throw new CfnNotStabilizedException(ResourceModel.TYPE_NAME, _model.getArn());
                            default: return false;
                        }
                    })
                    .progress()
                )
            .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
