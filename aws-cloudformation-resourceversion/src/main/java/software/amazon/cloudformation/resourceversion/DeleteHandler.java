package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class DeleteHandler extends BaseHandlerStd {
    private Logger logger;

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<CloudFormationClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        // pre-read to capture required metadata fields in model for Delete
        return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
            // now deregister the type
            .onSuccess(progress ->
                proxy.initiate("AWS-CloudFormation-ResourceVersion::Delete", proxyClient, progress.getResourceModel(), callbackContext)
                    .translateToServiceRequest(awsRequest ->
                        Translator.translateToDeleteRequest(awsRequest, logger))
                    .makeServiceCall((awsRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(awsRequest, proxyInvocation.client()::deregisterType))
                    .success());
    }

    private software.amazon.cloudformation.exceptions.ResourceNotFoundException nullSafeNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        return new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
