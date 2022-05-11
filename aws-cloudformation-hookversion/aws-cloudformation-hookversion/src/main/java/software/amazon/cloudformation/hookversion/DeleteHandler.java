package software.amazon.cloudformation.hookversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class DeleteHandler extends BaseHandlerStd {
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();

        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
                proxy.newInitiator(proxyClient, resourceModel, callbackContext);

        logger.log(String.format("Deregistering the hook version with identifier %s", resourceModel.getArn()));
        // pre-read to capture required metadata fields in model for Delete
        return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
                // now deregister the type
                .onSuccess(progress ->
                        initiator.initiate("AWS-CloudFormation-HookVersion::Delete")
                                .translateToServiceRequest(model ->
                                        Translator.translateToDeleteRequest(progress.getResourceModel(), logger))
                                .makeServiceCall((awsRequest, proxyInvocation) ->
                                        deregisterResource(awsRequest, proxyInvocation, progress.getResourceModel(), logger))
                                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.SUCCESS)
                                        .build()));
    }

    private DeregisterTypeResponse deregisterResource(
            final DeregisterTypeRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model, Logger logger) {
        DeregisterTypeResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deregisterType);
        } catch (TypeNotFoundException exception) {
            logger.log(String.format("Failed to deregister the hook [%s] as it cannot be found %s", model.getPrimaryIdentifier().toString(), Arrays.toString(exception.getStackTrace())));
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        } catch (CfnRegistryException exception) {
            logger.log(
                    String.format("Failed to deregister hook with identifier %s:\n%s",
                            model.getPrimaryIdentifier().toString(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        logger.log(String.format("The hook [%s] is successfully deregistered ", model.getPrimaryIdentifier().toString()));
        return response;
    }
}
