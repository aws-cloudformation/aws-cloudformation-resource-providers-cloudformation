package software.amazon.cloudformation.stackset;

import com.google.common.annotations.VisibleForTesting;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.LimitExceededException;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotEmptyException;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperationStatus;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.MultipleOf;
import software.amazon.cloudformation.stackset.util.ClientBuilder;

import java.time.Duration;
import java.util.function.BiFunction;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetOperationRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.updateStackInstancesRequest;

/**
 * Placeholder for the functionality that could be shared across Create/Read/Update/Delete/List Handlers
 */
public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {


    protected static final int NO_CALLBACK_DELAY = 0;

    protected static final MultipleOf MULTIPLE_OF = MultipleOf.multipleOf()
            .multiple(2)
            .timeout(Duration.ofHours(24L))
            .delay(Duration.ofSeconds(2L))
            .build();

    protected static final BiFunction<ResourceModel, ProxyClient<CloudFormationClient>, ResourceModel>
            EMPTY_CALL = (model, proxyClient) -> model;

    @Override
    public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        return handleRequest(
                proxy,
                request,
                callbackContext != null ? callbackContext : new CallbackContext(),
                proxy.newProxy(ClientBuilder::getClient),
                logger
        );
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final ProxyClient<CloudFormationClient> proxyClient,
            final Logger logger);

    protected boolean filterException(AwsRequest request,
                                      Exception e,
                                      ProxyClient<CloudFormationClient> client,
                                      ResourceModel model,
                                      CallbackContext context) {
        return e instanceof OperationInProgressException | e instanceof StackSetNotEmptyException;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> createStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        callbackContext.getCreateStacksList().forEach(stackInstances -> proxy
                .initiate("AWS-CloudFormation-StackSet::CreateStackInstances", client, model, callbackContext)
                .request(modelRequest -> createStackInstancesRequest(modelRequest.getStackSetId(), modelRequest.getOperationPreferences(), stackInstances))
                .retry(MULTIPLE_OF)
                .call((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::createStackInstances))
                .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                .progress());

        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> deleteStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        callbackContext.getDeleteStacksList().forEach(stackInstances -> proxy
                .initiate("AWS-CloudFormation-StackSet::DeleteStackInstances", client, model, callbackContext)
                .request(modelRequest -> deleteStackInstancesRequest(modelRequest.getStackSetId(), modelRequest.getOperationPreferences(), stackInstances))
                .retry(MULTIPLE_OF)
                .call((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::deleteStackInstances))
                .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                .exceptFilter(this::filterException)
                .progress());

        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }

    protected ProgressEvent<ResourceModel, CallbackContext> updateStackInstances(
            final AmazonWebServicesClientProxy proxy,
            final ProxyClient<CloudFormationClient> client,
            final ProgressEvent<ResourceModel, CallbackContext> progress,
            final Logger logger) {

        final ResourceModel model = progress.getResourceModel();
        final CallbackContext callbackContext = progress.getCallbackContext();

        callbackContext.getUpdateStacksList().forEach(stackInstances -> proxy
                .initiate("AWS-CloudFormation-StackSet::UpdateStackInstances", client, model, callbackContext)
                .request(modelRequest -> updateStackInstancesRequest(modelRequest.getStackSetId(), modelRequest.getOperationPreferences(), stackInstances))
                .retry(MULTIPLE_OF)
                .call((modelRequest, proxyInvocation) -> proxyInvocation.injectCredentialsAndInvokeV2(modelRequest, proxyInvocation.client()::updateStackInstances))
                .stabilize((request, response, proxyInvocation, resourceModel, context) -> isOperationStabilized(proxyInvocation, resourceModel, response.operationId(), logger))
                .exceptFilter(this::filterException)
                .progress());

        return ProgressEvent.defaultInProgressHandler(callbackContext, NO_CALLBACK_DELAY, model);
    }

    /**
     * Get {@link StackSet} from service client using stackSetId
     * @param stackSetId StackSet Id
     * @return {@link StackSet}
     */
    protected StackSet describeStackSet(
            final ProxyClient<CloudFormationClient> proxyClient,
            final String stackSetId) {

        final DescribeStackSetResponse stackSetResponse = proxyClient.injectCredentialsAndInvokeV2(
                describeStackSetRequest(stackSetId), proxyClient.client()::describeStackSet);
        return stackSetResponse.stackSet();
    }

    /**
     * Checks if the operation is stabilized using OperationId to interact with
     * {@link DescribeStackSetOperationResponse}
     * @param model {@link ResourceModel}
     * @param operationId OperationId from operation response
     * @param logger Logger
     * @return A boolean value indicates if operation is complete
     */
    protected boolean isOperationStabilized(final ProxyClient<CloudFormationClient> proxyClient,
                                            final ResourceModel model,
                                            final String operationId,
                                            final Logger logger) {

        final String stackSetId = model.getStackSetId();
        final StackSetOperationStatus status = getStackSetOperationStatus(proxyClient, stackSetId, operationId);
        return isStackSetOperationDone(status, operationId, logger);
    }


    /**
     * Retrieves the {@link StackSetOperationStatus} from {@link DescribeStackSetOperationResponse}
     * @param stackSetId {@link ResourceModel#getStackSetId()}
     * @param operationId Operation ID
     * @return {@link StackSetOperationStatus}
     */
    private static StackSetOperationStatus getStackSetOperationStatus(
            final ProxyClient<CloudFormationClient> proxyClient,
            final String stackSetId,
            final String operationId) {

        final DescribeStackSetOperationResponse response = proxyClient.injectCredentialsAndInvokeV2(
                describeStackSetOperationRequest(stackSetId, operationId),
                proxyClient.client()::describeStackSetOperation);
        return response.stackSetOperation().status();
    }

    /**
     * Compares {@link StackSetOperationStatus} with specific statuses
     * @param status {@link StackSetOperationStatus}
     * @param operationId Operation ID
     * @return boolean
     */
    @VisibleForTesting
    protected static boolean isStackSetOperationDone(
            final StackSetOperationStatus status, final String operationId, final Logger logger) {

        switch (status) {
            case SUCCEEDED:
                logger.log(String.format("%s has been successfully stabilized.", operationId));
                return true;
            case RUNNING:
            case QUEUED:
                return false;
            default:
                logger.log(String.format("StackInstanceOperation [%s] unexpected status [%s]", operationId, status));
                throw new TerminalException(
                        String.format("Stack set operation [%s] was unexpectedly stopped or failed", operationId));
        }
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleException(AwsRequest request,
                                                                          Exception exception,
                                                                          ProxyClient<CloudFormationClient> client,
                                                                          ResourceModel model,
                                                                          CallbackContext context,
                                                                          AmazonWebServicesClientProxy proxy) {

        return proxy.defaultHandler(request, exception, client, model, context);
    }
}
