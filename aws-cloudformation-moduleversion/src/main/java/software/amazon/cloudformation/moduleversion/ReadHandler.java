package software.amazon.cloudformation.moduleversion;

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

        logger.log(String.format("Reading module version with identifier %s", model.getPrimaryIdentifier().toString()));
        return proxy.initiate("AWS-CloudFormation-ModuleVersion::Read", proxyClient, model, callbackContext)
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((describeTypeRequest, sdkProxyClient) -> readModule(describeTypeRequest, sdkProxyClient, model, logger))
                .done(describeTypeResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(describeTypeResponse)));
    }

    private DescribeTypeResponse readModule(
            final DescribeTypeRequest describeTypeRequest,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model,
            final Logger logger) {

        DescribeTypeResponse describeTypeResponse;
        try {
            describeTypeResponse = proxyClient.injectCredentialsAndInvokeV2(describeTypeRequest, proxyClient.client()::describeType);
        } catch (final TypeNotFoundException exception) {
            logger.log(String.format("Module with identifier %s Not Found", model.getPrimaryIdentifier().toString()));
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to read module in registry:\n%s", Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }

        // if the type is deprecated, this will be treated as non-existent for the purposes of CloudFormation
        if (describeTypeResponse.deprecatedStatus() == DeprecatedStatus.DEPRECATED) {
            logger.log(String.format("Module with identifier %s is deprecated: Not Found", model.getPrimaryIdentifier().toString()));
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, model.getPrimaryIdentifier().toString());
        }
        return describeTypeResponse;
    }
}
