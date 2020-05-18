package software.amazon.cloudformation.resourceversionalias;

import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class UpdateHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> request;
    private CallbackContext callbackContext;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        this.proxy = proxy;
        this.request = request;
        this.callbackContext = callbackContext;
        this.logger = logger;

        final ResourceModel model = request.getDesiredResourceState();

        try {
            proxy.injectCredentialsAndInvokeV2(Translator.translateToUpdateRequest(model, logger),
                ClientBuilder.getClient()::setTypeDefaultVersion);
        } catch (final ResourceNotFoundException e) {
            throw nullSafeNotFoundException(model);
        }

        final String message = String.format("%s successfully set default version to %s.",
            ResourceModel.TYPE_NAME, model.getArn());
        logger.log(message);

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private software.amazon.cloudformation.exceptions.ResourceNotFoundException nullSafeNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        return new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
