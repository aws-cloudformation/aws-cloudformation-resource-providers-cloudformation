package software.amazon.cloudformation.hooktypeconfig;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
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

        logger.log(String.format("Disabling [ConfigurationArn: %s | Type: %s]",
                resourceModel.getConfigurationArn(), resourceModel.getTypeName()));

        return proxy.initiate("AWS-CloudFormation-HookTypeConfig::Delete", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .makeServiceCall((setTypeConfigurationRequest, client) -> proxyClient.injectCredentialsAndInvokeV2(setTypeConfigurationRequest, proxyClient.client()::setTypeConfiguration))
                .handleError((setTypeConfigurationRequest, exception, clientProxy, model, context) -> {
                    if (exception instanceof TypeNotFoundException) {
                        logger.log(String.format("Failed to set hook type configuration [%s] as it cannot be found %s", model.getConfigurationArn(), Arrays.toString(exception.getStackTrace())));
                        throw new CfnNotFoundException(exception);
                    } else {
                        logger.log(String.format("Failed to set hook type configuration [%s] and the exception is [%s]", model.getConfigurationArn(), Arrays.toString(exception.getStackTrace())));
                        throw new CfnGeneralServiceException(exception);
                    }
                })
                .done(awsResponse -> ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.SUCCESS)
                        .build());
    }
}
