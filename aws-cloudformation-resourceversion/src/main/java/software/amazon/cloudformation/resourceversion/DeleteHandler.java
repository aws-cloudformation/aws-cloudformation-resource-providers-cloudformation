package software.amazon.cloudformation.resourceversion;

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

        logger.log(String.format("Deregistering the resource version with identifier %s", resourceModel.getArn()));

        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress ->
                        initiator.initiate("AWS-CloudFormation-ResourceVersion::Delete")
                                .translateToServiceRequest(model ->
                                        Translator.translateToDeleteRequest(progress.getResourceModel(), logger))
                                .makeServiceCall((awsRequest, proxyInvocation) ->
                                        deregisterResource(awsRequest,proxyInvocation, progress.getResourceModel()))
                                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                                        .status(OperationStatus.SUCCESS)
                                        .build()));
    }

    private DeregisterTypeResponse deregisterResource(
            final DeregisterTypeRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model) {
        DeregisterTypeResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deregisterType);
        } catch (TypeNotFoundException exception) {
            logger.log(String.format("Failed to deregister the resource [%s] as it cannot be found %s", model.getArn(), Arrays.toString(exception.getStackTrace())));
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getArn());
        } catch (CfnRegistryException exception) {
            logger.log(
                    String.format("Failed to deregister resource with identifier %s:\n%s",
                            model.getArn(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        } catch (Exception exception){
            logger.log(String.format("Failed to deregister the resource [%s] with a GeneralService exception %s", model.getArn(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(ResourceModel.TYPE_NAME, exception);
        }
        logger.log(String.format("The resource [%s] is successfully deregistered ", model.getArn()));
        return response;
    }
}
