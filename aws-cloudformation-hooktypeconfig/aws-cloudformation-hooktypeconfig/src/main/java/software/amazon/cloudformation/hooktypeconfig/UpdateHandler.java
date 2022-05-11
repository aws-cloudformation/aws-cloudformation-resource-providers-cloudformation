package software.amazon.cloudformation.hooktypeconfig;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class UpdateHandler extends BaseHandlerStd {

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

        final String pattern = "^arn:aws[A-Za-z0-9-]{0,64}:cloudformation:[A-Za-z0-9-]{1,64}:([0-9]{12})?:type/hook/.+$";
        if (!StringUtils.isNullOrEmpty(resourceModel.getConfigurationArn()) && resourceModel.getConfigurationArn().matches(pattern)){
            throw new CfnGeneralServiceException("Primary Id for this resource is changed. To fix your stack, please remove this resource from the stack, perform stack update operation. Then, re-add this resource to continue regular operations of your stack. This is a one time change.");
        }

        logger.log(String.format("Updating [ConfigurationArn: %s | Type: %s]",
                resourceModel.getConfigurationArn(), resourceModel.getTypeName()));

        return proxy.initiate("AWS-CloudFormation-HookTypeConfig::Update", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToUpdateRequest)
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
                .done(awsResponse -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
