package software.amazon.cloudformation.moduledefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class ReadHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel model = request.getDesiredResourceState();
        validateModel(model);

        return proxy.initiate("AWS-CloudFormation-ModuleDefaultVersion::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((describeTypeRequest, proxyClient1) -> describeModule(describeTypeRequest, proxyClient, model, logger))
                .done(describeTypeResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describeTypeResponse)));
    }

    private DescribeTypeResponse describeModule(
            final DescribeTypeRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model,
            final Logger logger) {

        DescribeTypeResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::describeType);

            if (!response.isDefaultVersion() || response.deprecatedStatus() == DeprecatedStatus.DEPRECATED) {
                logger.log(
                        String.format("Module with identifier %s Not Found: module " + (!response.isDefaultVersion() ? "is not default version" : "is deprecated"),
                        model.getPrimaryIdentifier().toString()));
                throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
            }
        } catch (final TypeNotFoundException exception) {
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to read module in registry:\n%s", Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }
}
