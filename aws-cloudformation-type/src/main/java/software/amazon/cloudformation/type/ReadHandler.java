package software.amazon.cloudformation.type;

import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
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

        return fetchTypeAndAssertExists();
    }

    private ProgressEvent<ResourceModel, CallbackContext> fetchTypeAndAssertExists() {
        final ResourceModel model = request.getDesiredResourceState();

        if (model == null ||
            (model.getTypeName() == null && model.getArn() == null)) {
            throw nullSafeNotFoundException(model);
        }

        DescribeTypeResponse response = null;
        try {
            response = proxy.injectCredentialsAndInvokeV2(Translator.translateToReadRequest(model),
                ClientBuilder.getClient()::describeType);

            // if the type is deprecated, this will be treated as non-existent for the purposes of CloudFormation
            if (response.deprecatedStatus() == DeprecatedStatus.DEPRECATED) {
                throw nullSafeNotFoundException(model);
            }
        } catch (final TypeNotFoundException e) {
            throw nullSafeNotFoundException(model);
        } catch (final CfnRegistryException e) {
            logger.log(Arrays.toString(e.getStackTrace()));
            throw new CfnGeneralServiceException(e);
        }

        final ResourceModel modelFromReadResult = Translator.translateForRead(response);

        return ProgressEvent.defaultSuccessHandler(modelFromReadResult);
    }

    private software.amazon.cloudformation.exceptions.ResourceNotFoundException nullSafeNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        return new software.amazon.cloudformation.exceptions.ResourceNotFoundException(ResourceModel.TYPE_NAME,
            Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
