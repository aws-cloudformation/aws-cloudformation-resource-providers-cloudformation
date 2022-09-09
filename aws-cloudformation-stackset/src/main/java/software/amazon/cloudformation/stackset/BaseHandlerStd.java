package software.amazon.cloudformation.stackset;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.StackInstanceNotFoundException;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperation;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperationStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSetStatus;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesResponse;
import software.amazon.cloudformation.Action;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.MultipleOf;
import software.amazon.cloudformation.stackset.util.AltResourceModelAnalyzer;
import software.amazon.cloudformation.stackset.util.ClientBuilder;
import software.amazon.cloudformation.stackset.util.InstancesAnalyzer;
import software.amazon.cloudformation.stackset.util.StackInstancesPlaceHolder;
import software.amazon.cloudformation.stackset.util.Validator;

import java.time.Duration;
import java.util.List;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetOperationRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.updateStackInstancesRequest;
import static software.amazon.cloudformation.stackset.util.Comparator.isAccountLevelTargetingEnabled;

/**
 * Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers
 */
public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

    protected static final MultipleOf MULTIPLE_OF = MultipleOf.multipleOf()
            .multiple(2)
            .timeout(Duration.ofHours(24L))
            .delay(Duration.ofSeconds(2L))
            .build();

    /**
     * Retrieves the {@link StackSetOperation} from {@link DescribeStackSetOperationResponse}
     *
     * @param stackSetId  {@link ResourceModel#getStackSetId()}
     * @param operationId Operation ID
     * @return {@link StackSetOperation}
     */
    private static StackSetOperation getStackSetOperation(
            final ProxyClient<CloudFormationClient> proxyClient,
            final String stackSetId,
            final String operationId,
            final String callAs,
            final Logger logger) {

        final DescribeStackSetOperationRequest request = describeStackSetOperationRequest(stackSetId, operationId, callAs);
        logger.log(String.format("%s [%s] DescribeStackSetOperation request: [%s]",
                ResourceModel.TYPE_NAME, stackSetId, request));
        final DescribeStackSetOperationResponse response = proxyClient.injectCredentialsAndInvokeV2(request,
                proxyClient.client()::describeStackSetOperation);
        return response.stackSetOperation();
    }

    /**
     * Compares {@link StackSetOperationStatus} with specific statuses
     *
     * @param stackSetOperation {@link StackSetOperation}
     * @param operationId       Operation ID
     * @param stackSetId        StackSet ID
     * @return boolean
     */
    @VisibleForTesting
    protected static boolean isStackSetOperationDone(
            final StackSetOperation stackSetOperation, final String operationId, final String stackSetId, final Logger logger) {
        StackSetOperationStatus status = stackSetOperation.status();
        String statusReason = stackSetOperation.statusReason();
        switch (status) {
            case SUCCEEDED:
                logger.log(String.format("StackSet Operation [%s] has been successfully stabilized.", operationId));
                return true;
            case RUNNING:
            case QUEUED:
                return false;
            default:
                logger.log(String.format("StackSet Operation [%s] unexpected status [%s] status reason [%s]", operationId, status, statusReason));
                throw new CfnNotStabilizedException(
                        String.format("Stack set operation [%s] was unexpectedly stopped or failed. status reason: [%s]", operationId, statusReason), stackSetId);
        }
    }

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return handleRequest(proxy, request, callbackContext != null ?
                callbackContext : new CallbackContext(), proxy.newProxy(ClientBuilder::getClient), logger);
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger);

    protected boolean filterException(AwsRequest request, Exception e, ProxyClient<CloudFormationClient> client, ResourceModel model, CallbackContext context) {
        return e instanceof OperationInProgressException;
    }

    /**
     * Invocation of CreateStackInstances would possibly used by CREATE/UPDATE handler, after the template being analyzed
     * by {@link InstancesAnalyzer}
     *
     * @param proxy              {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client             the aws service client {@link ProxyClient<CloudFormationClient>} to make the call
     * @param progress           {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @param stackInstancesList StackInstances that need to create, see in {@link InstancesAnalyzer#analyzeForCreate}
     * @param logger             {@link Logger}
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */
    protected ProgressEvent<ResourceModel, CallbackContext> createStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final List<StackInstances> stackInstancesList,
            final Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        for (final StackInstances stackInstances : stackInstancesList) {
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate("AWS-CloudFormation-StackSet::CreateStackInstances" + stackInstances.hashCode(), client, model, callbackContext)
                    .translateToServiceRequest(modelRequest -> createStackInstancesRequest(modelRequest.getStackSetId(), modelRequest.getOperationPreferences(), stackInstances, modelRequest.getCallAs()))
                    .backoffDelay(MULTIPLE_OF)
                    .makeServiceCall((modelRequest, proxyInvocation) -> {
                        logger.log(String.format("%s [%s] CreateStackInstances request: [%s]", ResourceModel.TYPE_NAME, model.getStackSetId(), modelRequest));
                        final CreateStackInstancesResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::createStackInstances);
                        logger.log(String.format("%s [%s] CreateStackInstances in [%s] of [%s] initiated", ResourceModel.TYPE_NAME, model.getStackSetId(), stackInstances.getRegions(), stackInstances.getDeploymentTargets()));
                        return response;
                    })
                    .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                    .success();

            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }

        return ProgressEvent.progress(model, callbackContext);
    }

    /**
     * Invocation of DeleteStackInstances would possibly used by UPDATE/DELETE handler, after the template being analyzed
     * by {@link InstancesAnalyzer}
     *
     * @param proxy              {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client             the aws service client {@link ProxyClient<CloudFormationClient>} to make the call
     * @param progress           {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @param stackInstancesList StackInstances that need to create, see in {@link InstancesAnalyzer#analyzeForDelete}
     * @param logger             {@link Logger}
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */
    protected ProgressEvent<ResourceModel, CallbackContext> deleteStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final List<StackInstances> stackInstancesList,
            final Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        for (final StackInstances stackInstances : stackInstancesList) {
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate("AWS-CloudFormation-StackSet::DeleteStackInstances" + stackInstances.hashCode(), client, model, callbackContext)
                    .translateToServiceRequest(modelRequest -> deleteStackInstancesRequest(modelRequest.getStackSetId(), modelRequest.getOperationPreferences(), stackInstances, modelRequest.getCallAs()))
                    .backoffDelay(MULTIPLE_OF)
                    .makeServiceCall((modelRequest, proxyInvocation) -> {
                        logger.log(String.format("%s [%s] DeleteStackInstances request: [%s]", ResourceModel.TYPE_NAME, model.getStackSetId(), modelRequest));
                        final DeleteStackInstancesResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::deleteStackInstances);
                        logger.log(String.format("%s [%s] DeleteStackInstances in [%s] of [%s] initiated", ResourceModel.TYPE_NAME, model.getStackSetId(), stackInstances.getRegions(), stackInstances.getDeploymentTargets()));
                        return response;
                    })
                    .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                    .handleError((request, e, proxyClient, model_, context) -> {
                        // If StackInstanceNotFoundException is thrown by the service, then we did succeed delete/stabilization call in case of out of band deletion.
                        if (e instanceof StackInstanceNotFoundException) {
                            return ProgressEvent.success(model_, context);
                        }
                        // If OperationInProgressException is thrown by the service, then we retry
                        if (e instanceof OperationInProgressException) {
                            return ProgressEvent.progress(model_, context);
                        }
                        throw e;
                    })
                    .success();

            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }

        return ProgressEvent.progress(model, callbackContext);
    }

    /**
     * Invocation of DeleteStackInstances would possibly used by DELETE handler, after the template being analyzed
     * by {@link InstancesAnalyzer}
     *
     * @param proxy              {@link AmazonWebServicesClientProxy} to initiate proxy chain
     * @param client             the aws service client {@link ProxyClient<CloudFormationClient>} to make the call
     * @param progress           {@link ProgressEvent<ResourceModel, CallbackContext>} to place hold the current progress data
     * @param stackInstancesList StackInstances that need to create, see in {@link InstancesAnalyzer#analyzeForUpdate}
     * @param logger             {@link Logger}
     * @return {@link ProgressEvent<ResourceModel, CallbackContext>}
     */
    protected ProgressEvent<ResourceModel, CallbackContext> updateStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final List<StackInstances> stackInstancesList,
            final Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        for (final StackInstances stackInstances : stackInstancesList) {
            final ProgressEvent<ResourceModel, CallbackContext> progressEvent = proxy
                    .initiate("AWS-CloudFormation-StackSet::UpdateStackInstances" + stackInstances.hashCode(), client, model, callbackContext)
                    .translateToServiceRequest(modelRequest -> updateStackInstancesRequest(modelRequest.getStackSetId(), modelRequest.getOperationPreferences(), stackInstances, modelRequest.getCallAs()))
                    .backoffDelay(MULTIPLE_OF)
                    .makeServiceCall((modelRequest, proxyInvocation) -> {
                        logger.log(String.format("%s [%s] UpdateStackInstances request: [%s]", ResourceModel.TYPE_NAME, model.getStackSetId(), modelRequest));
                        final UpdateStackInstancesResponse response = proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::updateStackInstances);
                        logger.log(String.format("%s [%s] UpdateStackInstances in [%s] of [%s] initiated", ResourceModel.TYPE_NAME, model.getStackSetId(), stackInstances.getRegions(), stackInstances.getDeploymentTargets()));
                        return response;
                    })
                    .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                    .retryErrorFilter(this::filterException)
                    .success();

            if (!progressEvent.isSuccess()) {
                return progressEvent;
            }
        }

        return ProgressEvent.progress(model, callbackContext);
    }

    protected StackSet describeStackSet(final ProxyClient<CloudFormationClient> proxyClient,
                                        final String stackSetId,
                                        final Logger logger) {
        return describeStackSet(proxyClient, stackSetId, null, logger);
    }

    /**
     * Get {@link StackSet} from service client using stackSetId
     *
     * @param stackSetId    StackSet Id
     * @param callAs        CallAS
     * @param logger        {@link Logger}
     * @throws CfnNotFoundException If the StackSet is DELETED, return NotFound exception
     * @return {@link StackSet}
     */
    protected StackSet describeStackSet(
            final ProxyClient<CloudFormationClient> proxyClient,
            final String stackSetId,
            final String callAs,
            final Logger logger) {

        final DescribeStackSetRequest request = describeStackSetRequest(stackSetId, callAs);
        logger.log(String.format("%s [%s] DescribeStackSet request: [%s]", ResourceModel.TYPE_NAME, stackSetId, request));
        final DescribeStackSetResponse stackSetResponse = proxyClient.injectCredentialsAndInvokeV2(
                request, proxyClient.client()::describeStackSet);
        logger.log(String.format("Describe StackSet [%s] successfully", stackSetId));
        final StackSet stackSet = stackSetResponse.stackSet();
        // Apparently, deleted StackSets would be still retrievable using identifier StackSetId
        // We would need to throw CfnNotFoundException in this case for contract test
        if (StackSetStatus.DELETED == stackSet.status()) {
            logger.log(String.format("StackSet [%s] is %s", stackSetId, StackSetStatus.DELETED.toString()));
            throw new CfnNotFoundException(ResourceModel.TYPE_NAME, stackSetId);
        }
        return stackSet;
    }

    /**
     * Checks if the operation is stabilized using OperationId to interact with
     * {@link DescribeStackSetOperationResponse}
     *
     * @param model       {@link ResourceModel}
     * @param operationId OperationId from operation response
     * @param logger      Logger
     * @return A boolean value indicates if operation is complete
     */
    protected boolean isOperationStabilized(final ProxyClient<CloudFormationClient> proxyClient,
                                            final ResourceModel model,
                                            final String operationId,
                                            final Logger logger) {

        final String stackSetId = model.getStackSetId();
        final String callAs = model.getCallAs();
        final StackSetOperation stackSetOperation = getStackSetOperation(proxyClient, stackSetId, operationId, callAs, logger);
        return isStackSetOperationDone(stackSetOperation, operationId, stackSetId, logger);
    }

    /**
     * Analyzes/validates template and StackInstancesGroup
     *
     * @param proxyClient the aws service client {@link ProxyClient<CloudFormationClient>} to make the call
     * @param request     {@link ResourceHandlerRequest<ResourceModel>}
     * @param placeHolder {@link StackInstancesPlaceHolder}
     * @param action      {@link Action}
     */
    protected void analyzeTemplate(
            final ProxyClient<CloudFormationClient> proxyClient,
            final ResourceHandlerRequest<ResourceModel> request,
            final StackInstancesPlaceHolder placeHolder,
            final Action action) {

        final ResourceModel desiredModel = request.getDesiredResourceState();
        final ResourceModel previousModel = request.getPreviousResourceState();

        /*
        * If previous or desired model enables ALT, will apply AltResourceModelAnalyzer
        * */
        if (isAccountLevelTargetingEnabled(previousModel) || isAccountLevelTargetingEnabled(desiredModel)){
            switch (action) {
                /*
                * For create -- it's equivalent to compare the desired model to an empty model.
                * In other words, only set AltResourceModelAnalyzer currentModel() parameter.
                * */
                case CREATE:
                    new Validator().validateTemplate(proxyClient, desiredModel.getTemplateBody(), desiredModel.getTemplateURL());
                    AltResourceModelAnalyzer.builder().currentModel(desiredModel).build().analyze(placeHolder);
                    break;
                /*
                * For update -- compare current and previous model.
                * */
                case UPDATE:
                    new Validator().validateTemplate(proxyClient, desiredModel.getTemplateBody(), desiredModel.getTemplateURL());
                    AltResourceModelAnalyzer.builder().currentModel(desiredModel).previousModel(previousModel).build().analyze(placeHolder);
                    break;
                /*
                * For delete -- it's equivalent to compare an empty model to the desiredModel.
                * In other words, only set AltResourceModelAnalyzer previousModel() parameter.
                * */
                case DELETE:
                    AltResourceModelAnalyzer.builder().previousModel(desiredModel).build().analyze(placeHolder);
                    break;
            }
            return;
        }

        switch (action) {
            case CREATE:
                new Validator().validateTemplate(proxyClient, desiredModel.getTemplateBody(), desiredModel.getTemplateURL());
                InstancesAnalyzer.builder().desiredModel(desiredModel).build().analyzeForCreate(placeHolder);
                break;
            case UPDATE:
                new Validator().validateTemplate(proxyClient, desiredModel.getTemplateBody(), desiredModel.getTemplateURL());
                InstancesAnalyzer.builder().desiredModel(desiredModel).previousModel(previousModel).build().analyzeForUpdate(placeHolder);
                break;
            case DELETE:
                InstancesAnalyzer.builder().desiredModel(desiredModel).build().analyzeForDelete(placeHolder);
            }
    }

}
