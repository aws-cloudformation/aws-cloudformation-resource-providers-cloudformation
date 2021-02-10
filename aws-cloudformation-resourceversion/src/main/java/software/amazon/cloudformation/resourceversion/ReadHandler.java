package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeTypeResponse;
import software.amazon.awssdk.services.cloudformation.model.TypeNotFoundException;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.CallChain;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Objects;

public class ReadHandler extends BaseHandlerStd {

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        final ResourceModel resourceModel = request.getDesiredResourceState();
        final CallChain.Initiator<CloudFormationClient, ResourceModel, CallbackContext> initiator =
                proxy.newInitiator(proxyClient, resourceModel, callbackContext);
        logger.log(String.format("Reading the resource version with identifier %s", resourceModel.getArn()));
        return initiator.initiate("AWS-CloudFormation-ResourceVersion::Read")
                .translateToServiceRequest((model) -> Translator.translateToReadRequest(model, logger))
                .makeServiceCall((awsRequest, sdkProxyClient) -> readResource(awsRequest, sdkProxyClient, resourceModel))
                .done(awsResponse -> ProgressEvent.defaultSuccessHandler(Translator.translateFromReadResponse(awsResponse)));
    }

    private DescribeTypeResponse readResource(
            final DescribeTypeRequest describeTypeRequest,
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceModel model) {

        DescribeTypeResponse awsResponse;
        try {
            awsResponse = proxyClient.injectCredentialsAndInvokeV2(describeTypeRequest, proxyClient.client()::describeType);

            // if the type is deprecated, this will be treated as non-existent for the purposes of CloudFormation
            if (awsResponse.deprecatedStatus() == DeprecatedStatus.DEPRECATED) {
                throw nullSafeNotFoundException(model);
            }
        } catch (final TypeNotFoundException e) {
            logger.log(String.format("Failed to read the resource [%s] as it cannot be found", model.getPrimaryIdentifier().toString()));
            throw nullSafeNotFoundException(model);
        } catch (final CfnRegistryException e) {
            logger.log(String.format("Failed to read the resource [%s] due to a CfnRegistryException",Arrays.toString(e.getStackTrace())));
            throw new CfnGeneralServiceException(e);
        }

        return awsResponse;
    }

    private CfnNotFoundException nullSafeNotFoundException(final ResourceModel model) {
        final ResourceModel nullSafeModel = model == null ? ResourceModel.builder().build() : model;
        return new CfnNotFoundException(ResourceModel.TYPE_NAME,
                Objects.toString(nullSafeModel.getPrimaryIdentifier()));
    }
}
