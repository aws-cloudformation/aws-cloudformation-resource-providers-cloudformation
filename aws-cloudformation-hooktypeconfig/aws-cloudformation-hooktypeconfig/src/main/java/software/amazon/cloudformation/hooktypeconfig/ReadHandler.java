package software.amazon.cloudformation.hooktypeconfig;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class ReadHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();

        logger.log(String.format("Reading [ConfigurationArn: %s | Type: %s]",
                resourceModel.getConfigurationArn(), resourceModel.getTypeName()));

        return proxy.initiate("AWS-CloudFormation-HookTypeConfig::Read", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((batchDescribeTypeConfigurationsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(batchDescribeTypeConfigurationsRequest, sdkProxyClient.client()::batchDescribeTypeConfigurations))
                .handleError((describeTypeRequest, exception, clientProxy, resourcemodel, context) -> {
                    logger.log(String.format("Failed to Read the hook type configuration [%s] and the exception is [%s]", resourcemodel.getConfigurationArn(), Arrays.toString(exception.getStackTrace())));
                    throw new CfnGeneralServiceException(exception);
                })
                .done(batchDescribeTypeConfigurationsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(batchDescribeTypeConfigurationsResponse, logger)));
    }
}
