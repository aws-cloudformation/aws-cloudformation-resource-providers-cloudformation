package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
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

        // https://github.com/aws-cloudformation/cloudformation-cli-java-plugin/blob/master/src/main/java/software/amazon/cloudformation/proxy/CallChain.java

        // STEP 1 [initialize a proxy context]
        return proxy.initiate("AWS-CloudFormation-Stack::Read", proxyClient, request.getDesiredResourceState(), callbackContext)

            .translateToServiceRequest(Translator::translateToReadRequest)

            // Implement client invocation of the read request through the proxyClient, which is already initialised with
            // caller credentials, correct region and retry settings
            .makeServiceCall((awsRequest, client) -> {
                DescribeStacksResponse awsResponse ;
                try {
                    awsResponse = client.injectCredentialsAndInvokeV2(awsRequest,client.client()::describeStacks);
                } catch (final AwsServiceException e) { // ResourceNotFoundException
                    /*
                    * While the handler contract states that the handler must always return a progress event,
                    * you may throw any instance of BaseHandlerException, as the wrapper map it to a progress event.
                    * Each BaseHandlerException maps to a specific error code, and you should map service exceptions as closely as possible
                    * to more specific error codes
                    */
                    throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, e); // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/commit/2077c92299aeb9a68ae8f4418b5e932b12a8b186#diff-5761e3a9f732dc1ef84103dc4bc93399R56-R63
                }
                if (awsResponse.stacks().isEmpty() || awsResponse.stacks().get(0).stackStatus() == StackStatus.DELETE_COMPLETE) {
                    throw new CfnNotFoundException(ResourceModel.TYPE_NAME, request.getDesiredResourceState().getStackId());
                }
                logger.log(String.format("%s has successfully been read.", ResourceModel.TYPE_NAME));
                return awsResponse;
            })

            // Implement client invocation of the read request through the proxyClient, which is already initialised with
            // caller credentials, correct region and retry settings
            .done(awsResponse -> ProgressEvent.defaultSuccessHandler(awsResponse.stacks().isEmpty()?
                request.getDesiredResourceState() : Translator.translateFromReadResponse(awsResponse.stacks().get(0))));
    }
}
