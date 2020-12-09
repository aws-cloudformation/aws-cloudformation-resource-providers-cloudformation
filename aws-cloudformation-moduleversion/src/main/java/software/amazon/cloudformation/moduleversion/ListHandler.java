package software.amazon.cloudformation.moduleversion;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CfnRegistryException;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypeVersionsResponse;
import software.amazon.awssdk.services.cloudformation.model.ListTypesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListTypesResponse;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.LinkedList;

public class ListHandler extends BaseHandlerStd {

    /**
     * The registry service does not have a native function to retrieve all versions of all types in one call.
     * This method is designed to retrieve the names of all modules, then use those names to list specific versions for
     * each module, if necessary.
     *
     * If the provided model contains the name of a module, only versions of that module will be listed.
     * If there is no provided model or the provided model does not specify a name, all versions of all modules are listed.
     */
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        ResourceModel model = request.getDesiredResourceState();
        if (model == null) {
            model = ResourceModel.builder().build();
            request.setDesiredResourceState(model);
        }

        if (request.getNextToken() != null && request.getNextToken().isEmpty()) {
            request.setNextToken(null);
        }

        ProgressEvent<ResourceModel, CallbackContext> progress;
        if (model.getModuleName() == null && callbackContext.getModuleToList() == null) {
            progress = listModuleDefaultVersions(request, callbackContext, proxyClient, logger);
        } else {
            progress = listModuleVersions(request, callbackContext, proxyClient, logger);
        }
        return progress;
    }

    private ProgressEvent<ResourceModel, CallbackContext> listModuleDefaultVersions(
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        logger.log("Listing modules");
        final ListTypesRequest listTypesRequest = Translator.translateToListTypesRequest(request.getNextToken());
        final ListTypesResponse listTypesResponse = listTypes(listTypesRequest, proxyClient, logger);
        return handleListTypesResponse(listTypesResponse, callbackContext, logger);
    }

    private ProgressEvent<ResourceModel, CallbackContext> listModuleVersions(
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        if (callbackContext.getModuleToList() == null) {
            callbackContext.setModuleToList(request.getDesiredResourceState());
        }
        final ResourceModel model = callbackContext.getModuleToList();
        logger.log(String.format("Listing module versions for module %s", model.getModuleName()));
        final ListTypeVersionsRequest listTypeVersionsRequest = Translator.translateToListTypeVersionsRequest(model, request.getNextToken(), callbackContext.getDeprecatedStatus());
        final ListTypeVersionsResponse listTypeVersionsResponse = listTypeVersions(listTypeVersionsRequest, proxyClient, logger);
        return handleListTypeVersionsResponse(listTypeVersionsResponse, callbackContext, logger);
    }

    private ListTypesResponse listTypes(
            final ListTypesRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        ListTypesResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::listTypes);
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to list modules:\n%s", Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }

    private ListTypeVersionsResponse listTypeVersions(
            final ListTypeVersionsRequest request,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger) {

        ListTypeVersionsResponse response;
        try {
            response = proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::listTypeVersions);
        } catch (final CfnRegistryException exception) {
            logger.log(String.format("Failed to list versions for module %s:\n%s", request.typeName(), Arrays.toString(exception.getStackTrace())));
            throw new CfnGeneralServiceException(exception);
        }
        return response;
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleListTypesResponse(
            final ListTypesResponse response,
            final CallbackContext callbackContext,
            final Logger logger) {

        callbackContext.addModulesToList(Translator.translateFromListTypesResponse(response));
        final ProgressEvent.ProgressEventBuilder<ResourceModel, CallbackContext> builder = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(callbackContext)
                .resourceModels(new LinkedList<>())
                .status(OperationStatus.SUCCESS);
        if (response.nextToken() == null) {
            logger.log("No nextToken in ListTypesResponse: moving to ListTypeVersions calls");
            builder.nextToken(""); // force another call of handleRequest to begin listing module versions
            if (callbackContext.hasModuleToList()) {
                callbackContext.setModuleToList(callbackContext.getModulesToList().remove(0));
            }
        } else {
            logger.log("Received nextToken in ListTypesResponse: another ListTypes call required");
            builder.nextToken(response.nextToken());
        }
        return builder.build();
    }

    private ProgressEvent<ResourceModel, CallbackContext> handleListTypeVersionsResponse(
            final ListTypeVersionsResponse response,
            final CallbackContext callbackContext,
            final Logger logger) {

        final String moduleToListModuleName = callbackContext.getModuleToList().getModuleName();

        final ProgressEvent.ProgressEventBuilder<ResourceModel, CallbackContext> builder = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .callbackContext(callbackContext)
                .resourceModels(Translator.translateFromListTypeVersionsResponse(response))
                .status(OperationStatus.SUCCESS);
        if (response.nextToken() == null) {

            if (callbackContext.hasModuleToList()) {
                callbackContext.setModuleToList(callbackContext.getModulesToList().remove(0));
                builder.nextToken(""); // force another call of handleRequest to list next module's versions
                logger.log(String.format("No more ListTypeVersions calls for %s: ListTypeVersions call required for %s next",
                        moduleToListModuleName, callbackContext.getModuleToList().getModuleName()));
            } else {
                logger.log("List operation complete");
                callbackContext.setModuleToList(null);
                builder.nextToken(null);
            }

        } else {
            logger.log(String.format("Received nextToken in ListTypeVersionsResponse for %s: another ListTypeVersions call required for %s",
                            moduleToListModuleName, moduleToListModuleName));
            builder.nextToken(response.nextToken());
        }
        return builder.build();
    }
}
