package software.amazon.cloudformation.resourcedefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
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

        logger.log(String.format("Reading [TypeVersionArn: %s | Type: %s | Version: %s]",
                resourceModel.getTypeVersionArn(), resourceModel.getTypeName(), resourceModel.getVersionId()));

        return proxy.initiate("AWS-CloudFormation-ResourceDefaultVersion::Read", proxyClient, resourceModel, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((awsRequest, sdkProxyClient) -> sdkProxyClient.injectCredentialsAndInvokeV2(awsRequest, sdkProxyClient.client()::describeType))
                .handleError((describeTypeRequest, exception, clientProxy, resourcemodel, context) -> {
                    if(exception instanceof TypeNotFoundException) {
                        logger.log(String.format("Failed to Read the resource [%s] as it cannot be found %s", resourcemodel.getPrimaryIdentifier().toString(), Arrays.toString(exception.getStackTrace())));
                        throw new CfnNotFoundException(exception);
                    }
                    else
                        throw exception;
                })
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }
}
