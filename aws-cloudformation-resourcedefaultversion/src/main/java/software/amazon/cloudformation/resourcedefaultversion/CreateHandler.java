package software.amazon.cloudformation.resourcedefaultversion;

import com.amazonaws.util.StringUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
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
        if (StringUtils.isNullOrEmpty(resourceModel.getArn())) {
            String generatedArn = createArn(request);
            resourceModel.setArn(generatedArn);
            return ProgressEvent.progress(resourceModel, callbackContext);
        }
        return ProgressEvent.progress(resourceModel, callbackContext)
                .then(progress -> {
                    final ResourceModel model = progress.getResourceModel();
                    logger.log(String.format("Creating [Arn: %s | Type: %s | Version: %s]",
                            model.getTypeVersionArn(), model.getTypeName(), model.getVersionId()));
                    return proxy.initiate("resourceDefaultVersion::Create", proxyClient, model, progress.getCallbackContext())
                            .translateToServiceRequest(Translator::translateToUpdateRequest)
                            .makeServiceCall((setTypeDefaultVersionRequest, client) -> proxyClient.injectCredentialsAndInvokeV2(setTypeDefaultVersionRequest, proxyClient.client()::setTypeDefaultVersion))
                            .progress();
                })
                .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
    }

    private String createArn(ResourceHandlerRequest<ResourceModel> request) {
        final ResourceModel resourceModel = request.getDesiredResourceState();
        if (!StringUtils.isNullOrEmpty(resourceModel.getTypeVersionArn())) {
            return resourceModel.getTypeVersionArn().substring(0, resourceModel.getTypeVersionArn().lastIndexOf("/"));
        } else {
            // generating Arn from the TypeName and versionID
            String arn = String.format("arn:%s:cloudformation:%s:%s:type/resource/%s",
                    request.getAwsPartition(),
                    request.getRegion(),
                    request.getAwsAccountId(),
                    resourceModel.getTypeName().replace("::", "-"));
            return arn;
        }
    }


}
