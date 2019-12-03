package software.amazon.cloudformation.typeversion;

import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.cloudformation.exceptions.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Objects;

public class ReadHandler extends BaseHandler<CallbackContext> {

    private AmazonWebServicesClientProxy proxy;
    private ResourceHandlerRequest<ResourceModel> request;
    private Logger logger;

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final Logger logger) {

        this.proxy = proxy;
        this.request = request;
        this.logger = logger;

        return fetchTypeVersionAndAssertExists();
    }

    private ProgressEvent<ResourceModel, CallbackContext> fetchTypeVersionAndAssertExists() {
        final ResourceModel model = request.getDesiredResourceState();

        DescribeTypeResponse response = null;
        try {
            response = proxy.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                ClientBuilder.getClient()::describeType);
        } catch (final ResourceNotFoundException e) {
            throwNotFoundException(model);
        }

        final ResourceModel modelFromReadResult = Translator.translateForRead(response);

        return ProgressEvent.defaultSuccessHandler(modelFromReadResult);
    }

    private void throwNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        throw new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
