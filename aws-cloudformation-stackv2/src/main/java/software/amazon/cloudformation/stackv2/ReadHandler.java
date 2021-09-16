package software.amazon.cloudformation.stackv2;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ReadHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        return proxy.initiate("AWS-CloudFormation-StackV2::Read", proxyClient, request.getDesiredResourceState(), callbackContext)
            .translateToServiceRequest(Translator::translateToReadRequest)
            .makeServiceCall((awsRequest, client) -> {
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
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(awsResponse.stacks().isEmpty() ?
                request.getDesiredResourceState() : Translator.translateFromReadResponse(awsResponse.stacks().get(0))));
    }
}
