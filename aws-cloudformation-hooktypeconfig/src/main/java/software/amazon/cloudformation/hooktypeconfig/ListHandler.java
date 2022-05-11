package software.amazon.cloudformation.hooktypeconfig;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class ListHandler extends BaseHandlerStd{
    @Override
    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(AmazonWebServicesClientProxy proxy,
                                                                          ResourceHandlerRequest<ResourceModel> request,
                                                                          CallbackContext callbackContext,
                                                                          ProxyClient<CloudFormationClient> proxyClient,
                                                                          Logger logger) {
        final ResourceModel resourceModel = request.getDesiredResourceState() == null
                ? ResourceModel.builder().build()
                : request.getDesiredResourceState();

        logger.log(String.format("List the configurations with the identifier %s", resourceModel.getTypeArn()));

        return proxy.initiate("AWS-CloudFormation-HookTypeConfig::List", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest((model) -> Translator.translateToListRequest(resourceModel))
                .makeServiceCall((batchDescribeTypeConfigurationsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(batchDescribeTypeConfigurationsRequest, sdkProxyClient.client()::batchDescribeTypeConfigurations))
                .handleError((listTypeVersionsRequest, exception, clientProxy, model, context) -> {
                    logger.log(String.format("Failed to list hook type versions [%s] and the exception is [%s]", model.getTypeArn(), Arrays.toString(exception.getStackTrace())));
                    throw new CfnGeneralServiceException(exception);
                })
                .done((batchDescribeTypeConfigurationsRequest, batchDescribeTypeConfigurationsResponse, sdkProxyClient, model, cc) ->
                        ProgressEvent.<ResourceModel, CallbackContext>builder()
                                .status(OperationStatus.SUCCESS)
                                .resourceModels(Translator.translateFromListResponse(batchDescribeTypeConfigurationsResponse, logger))
                                .build()
                );
    }
}
