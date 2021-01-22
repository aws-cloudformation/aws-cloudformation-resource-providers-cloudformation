package software.amazon.cloudformation.moduleversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class DeleteHandler extends BaseHandlerStd {

    private final ReadHandler readHandler;

    public DeleteHandler() {
        this(new ReadHandler());
    }

    DeleteHandler(final ReadHandler readHandler) {
        this.readHandler = readHandler;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        validateModel(model);

        logger.log(String.format("Deregistering module version with identifier %s", model.getPrimaryIdentifier().toString()));
        return readHandler.handleRequest(proxy, request, callbackContext, proxyClient, logger)
                .onSuccess(progress ->
                    proxy.initiate("AWS-CloudFormation-ModuleVersion::Delete", proxyClient, progress.getResourceModel(), callbackContext)
                            .translateToServiceRequest(Translator::translateToDeleteRequest)
                            .makeServiceCall((deregisterTypeRequest, proxyClient1) ->
                                    deregisterModule(deregisterTypeRequest, proxyClient, model))
                            .done(response -> ProgressEvent.defaultSuccessHandler(null)));
    }

    private DeregisterTypeResponse deregisterModule(
            final DeregisterTypeRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model) {
        DeregisterTypeResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deregisterType);
        } catch (TypeNotFoundException exception) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        } catch (CfnRegistryException exception) {
            logger.log(
                    String.format("Failed to deregister module with identifier %s:\n%s",
                            model.getPrimaryIdentifier().toString(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }
}
