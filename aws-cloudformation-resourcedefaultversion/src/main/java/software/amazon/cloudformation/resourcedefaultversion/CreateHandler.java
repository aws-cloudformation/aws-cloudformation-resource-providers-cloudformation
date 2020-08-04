package software.amazon.cloudformation.resourcedefaultversion;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionResponse;
import software.amazon.cloudformation.exceptions.CfnAlreadyExistsException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class CreateHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
            proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        logger.log(String.format("Creating [Arn: %s | Type: %s | Version: %s]",
            resourceModel.getArn(), resourceModel.getTypeName(), resourceModel.getVersionId()));

        return initiator
            .translateToServiceRequest(Translator::translateToUpdateRequest) // same API call for Create + Update
            .makeServiceCall((r, c) -> createResource(resourceModel, r, c))
            .done(setTypeDefaultVersionResponse -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private SetTypeDefaultVersionResponse createResource(
        final ResourceModel model,
        final SetTypeDefaultVersionRequest awsRequest,
        final ProxyClient<CloudFormationClient> proxyClient) {

        boolean exists = exists(proxyClient, model);
        if (exists) {
            this.logger.log("Default version for type is already set as requested");
            throw new CfnAlreadyExistsException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        }
        return proxyClient.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::setTypeDefaultVersion);
    }

    private boolean exists(final ProxyClient<CloudFormationClient> proxyClient,
                             final ResourceModel model) throws AwsServiceException {
        final DescribeTypeRequest translateToReadRequest = Translator.translateToReadRequest(model);
        final DescribeTypeResponse response = proxyClient.injectCredentialsAndInvokeV2(translateToReadRequest, proxyClient.client()::describeType);
        return response.isDefaultVersion();
    }
}
