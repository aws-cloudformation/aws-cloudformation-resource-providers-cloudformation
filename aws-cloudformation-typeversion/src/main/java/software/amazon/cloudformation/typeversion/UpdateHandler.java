package software.amazon.cloudformation.typeversion;

import software.amazon.awssdk.services.cloudformation.model.SetTypeDefaultVersionRequest;
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
        final SetTypeDefaultVersionRequest setTypeDefaultVersionRequest =
            Translator.translateToUpdateRequest(model);
        try {
            proxy.injectCredentialsAndInvokeV2(setTypeDefaultVersionRequest,
                ClientBuilder.getClient()::setTypeDefaultVersion);
        } catch (final ResourceNotFoundException e) {
            throwNotFoundException(model);
        }

        final String message =
            String.format("%s successfully set default version to %s.",
                ResourceModel.TYPE_NAME, model.getArn());
        logger.log(message);

        return ProgressEvent.defaultSuccessHandler(model);
    }

    private void throwNotFoundException(final ResourceModel model) {
        throw new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(model.getPrimaryIdentifier()));
    }
}
