package software.amazon.cloudformation.moduledefaultversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;

public class ListHandler extends BaseHandlerStd {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {
        final ListTypesResponse awsResponse = listTypes(request, proxyClient);

        final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                ProgressEvent.<ResourceModel, CallbackContext>builder().resourceModels(
                        Translator.translateFromListTypesResponse(awsResponse))
                        .nextToken(awsResponse.nextToken())
                        .status(OperationStatus.SUCCESS)
                        .build();
        return progressEvent;
    }

    private ListTypesResponse listTypes(ResourceHandlerRequest<ResourceModel> request,
            ProxyClient<CloudFormationClient> proxyClient) {
        ListTypesResponse awsResponse;

        try {
            awsResponse =
                    proxyClient.injectCredentialsAndInvokeV2(Translator.translateToListRequest(request.getNextToken()),
                            proxyClient.client()::listTypes);
            return awsResponse;
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to list modules:\n%s", Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
    }

}
