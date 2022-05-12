package software.amazon.cloudformation.hooktypeconfig;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.TypeConfigurationNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class CreateHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {
        final ResourceModel resourceModel = request.getDesiredResourceState();
        if (StringUtils.isNullOrEmpty(resourceModel.getTypeName())){
            resourceModel.setTypeName(resourceModel.getTypeArn()
                    .substring(resourceModel.getTypeArn().lastIndexOf("/") + 1)
                    .replace("-", "::"));
        }
        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress -> {
                    final ResourceModel model = progress.getResourceModel();
                    logger.log(String.format("Setting configuration for Type: %s | ConfigurationAlias: %s]",
                            resourceModel.getTypeName(), resourceModel.getConfigurationAlias()));
                    return proxy.initiate("AWS-CloudFormation-HookTypeConfig::Create", proxyClient, model, progress.getCallbackContext())
                            .translateToServiceRequest(Translator::translateToUpdateRequest)
                            .makeServiceCall((setTypeConfiguration, client) -> proxyClient.injectCredentialsAndInvokeV2(setTypeConfiguration, proxyClient.client()::setTypeConfiguration))
                            .handleError((setTypeConfigurationRequest, exception, clientProxy, resourcemodel, context) -> {
                                if(exception instanceof TypeConfigurationNotFoundException) {
                                    logger.log(String.format("Failed to set hook type configuration for type [%s] as it cannot be found %s", model.getTypeName(), Arrays.toString(exception.getStackTrace())));
                                    throw new CfnNotFoundException(exception);
                                }
                                else {
                                    logger.log(String.format("Failed to set hook type configuration for type [%s] and the exception is [%s]", model.getTypeName(), Arrays.toString(exception.getStackTrace())));
                                    throw new CfnGeneralServiceException(exception);
                                }
                            })
                            .progress();
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }
}
