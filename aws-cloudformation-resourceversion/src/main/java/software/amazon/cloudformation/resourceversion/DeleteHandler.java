package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeregisterTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class DeleteHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> request;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {
        final CallbackContext context = callbackContext == null ? CallbackContext.builder().build() : callbackContext;

        this.proxy = proxy;
        this.request = request;
        this.logger = logger;

        ProgressEvent<ResourceModel, CallbackContext> readResult;
        try {
            readResult = new ReadHandler().handleRequest(proxy, request, context, logger);
        } catch (CfnNotFoundException e) {
            throw nullSafeNotFoundException(request.getDesiredResourceState());
        }

        final ResourceModel model = readResult.getResourceModel();

        deregisterType(proxy, model, context, logger);

        return ProgressEvent.defaultSuccessHandler(null);
    }

    DeregisterTypeResponse deregisterType(
        final AmazonWebServicesClientProxy proxy,
        final ResourceModel model,
        final CallbackContext callbackContext,
        final Logger logger) {

        DeregisterTypeResponse response;

        try {
            response = proxy.injectCredentialsAndInvokeV2(Translator.translateToDeleteRequest(model, logger),
                ClientBuilder.getClient()::deregisterType);
            logger.log(String.format("%s [%s] successfully deleted.",
                ResourceModel.TYPE_NAME, model.getPrimaryIdentifier()));
        } catch (final TypeNotFoundException e) {
            throw nullSafeNotFoundException(model);
        } catch (final CfnRegistryException e) {
            throw new CfnGeneralServiceException("DeregisterType: " + e.getMessage());
        }

        return response;
    }

    private software.amazon.cloudformation.exceptions.ResourceNotFoundException nullSafeNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        return new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
