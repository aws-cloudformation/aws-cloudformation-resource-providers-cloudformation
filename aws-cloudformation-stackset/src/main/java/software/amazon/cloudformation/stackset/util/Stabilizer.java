package software.amazon.cloudformation.stackset.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationResponse;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperationStatus;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.exceptions.CfnServiceInternalErrorException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations;

import java.util.Map;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetOperationRequest;

/**
 * Utility class to help keeping track on stabilization status
 */
@AllArgsConstructor
@Builder
public class Stabilizer {

    private static final String INTERNAL_FAILURE = "Internal Failure";
    private static final int ONE_DAY_IN_SECONDS = 24 * 60 * 60;

    public static final Double RATE = 1.1;
    public static final int MAX_RETRIES = 60;
    public static final int BASE_CALLBACK_DELAY_SECONDS = 3;
    public static final int MAX_CALLBACK_DELAY_SECONDS = 30;
    public static final int EXECUTION_TIMEOUT_SECONDS = ONE_DAY_IN_SECONDS;

    private AmazonWebServicesClientProxy proxy;
    private CloudFormationClient client;
    private Logger logger;

    /**
     * Gets new exponential delay seconds based on {@link CallbackContext#getCurrentDelaySeconds},
     * However, the delay seconds will not exceed {@link Stabilizer#MAX_CALLBACK_DELAY_SECONDS}
     * @param context {@link CallbackContext}
     * @return New exponential delay seconds
     */
    public static int getDelaySeconds(final CallbackContext context) {
        final int currentDelaySeconds = context.getCurrentDelaySeconds();
        final int exponentialDelay = getExponentialDelay(currentDelaySeconds);
        context.setCurrentDelaySeconds(Math.min(MAX_CALLBACK_DELAY_SECONDS, exponentialDelay));
        return context.getCurrentDelaySeconds();
    }

    /**
     * Helper to get exponential delay seconds
     * @param delaySeconds current delay seconds
     * @return New exponential delay seconds
     */
    private static int getExponentialDelay(final int delaySeconds) {
        if (delaySeconds == 0) return BASE_CALLBACK_DELAY_SECONDS;
        final int exponentialDelay = (int) (delaySeconds * RATE);
        return delaySeconds == exponentialDelay ? delaySeconds + 1 : exponentialDelay;
    }

    /**
     * Checks if the operation is stabilized using {@link CallbackContext#getOperationId()} to interact with
     * {@link DescribeStackSetOperationResponse}
     * @param model {@link ResourceModel}
     * @param context {@link CallbackContext}
     * @return A boolean value indicates if operation is complete
     */
    public boolean isStabilized(final ResourceModel model, final CallbackContext context) {
        final String operationId = context.getOperationId();

        // If no stabilizing operation was run.
        if (operationId == null) return true;

        final String stackSetId = model.getStackSetId();
        final StackSetOperationStatus status = getStackSetOperationStatus(stackSetId, operationId);

        try {
            // If it exceeds max stabilization times
            if (context.incrementElapsedTime() > EXECUTION_TIMEOUT_SECONDS) {
                logger.log(String.format("StackSet stabilization [%s] time out", stackSetId));
                throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME);
            }

            // If it exceeds max retries
            if (context.getRetries() > MAX_RETRIES) {
                logger.log(String.format("StackSet stabilization [%s] reaches max retries", stackSetId));
                throw new CfnServiceInternalErrorException(ResourceModel.TYPE_NAME);
            }
            return isStackSetOperationDone(status, operationId);

        } catch (final CfnServiceInternalErrorException e) {
            throw new CfnNotStabilizedException(e);
        }
    }

    /**
     * Retrieves the {@link StackSetOperationStatus} from {@link DescribeStackSetOperationResponse}
     * @param stackSetId {@link ResourceModel#getStackSetId()}
     * @param operationId {@link CallbackContext#getOperationId()}
     * @return {@link StackSetOperationStatus}
     */
    private StackSetOperationStatus getStackSetOperationStatus(final String stackSetId, final String operationId) {
        final DescribeStackSetOperationResponse response = proxy.injectCredentialsAndInvokeV2(
                describeStackSetOperationRequest(stackSetId, operationId),
                client::describeStackSetOperation);
        return response.stackSetOperation().status();
    }

    /**
     * Compares {@link StackSetOperationStatus} with specific statuses
     * @param status {@link StackSetOperationStatus}
     * @param operationId {@link CallbackContext#getOperationId()}
     * @return Boolean
     */
    private Boolean isStackSetOperationDone(final StackSetOperationStatus status, final String operationId) {
        switch (status) {
            case SUCCEEDED:
                return true;
            case RUNNING:
            case QUEUED:
                return false;
            default:
                logger.log(String.format("StackInstanceOperation [%s] unexpected status [%s]", operationId, status));
                throw new CfnServiceInternalErrorException(
                        String.format("Stack set operation [%s] was unexpectedly stopped or failed", operationId));
        }
    }

    /**
     * Checks if this operation {@link UpdateOperations} needs to run at this stabilization runtime
     * @param isRequiredToRun If the operation is necessary to operate
     * @param isStabilizedStarted If the operation has been initialed
     * @param previousOperation Previous {@link UpdateOperations}
     * @param operation {@link UpdateOperations}
     * @param model {@link ResourceModel}
     * @param context {@link CallbackContext}
     * @return boolean
     */
    public boolean isPerformingOperation(
            final boolean isRequiredToRun,
            final boolean isStabilizedStarted,
            final UpdateOperations previousOperation,
            final UpdateOperations operation,
            final ResourceModel model,
            final CallbackContext context) {

        final Map<UpdateOperations, Boolean> operationsCompletionMap = context.getOperationsStabilizationMap();

        // if previousOperation is not done or this operation has completed
        if (!isPreviousOperationDone(context, previousOperation) || operationsCompletionMap.get(operation)) {
            return false;
        }

        // if it is not required to run, mark as complete
        if (!isRequiredToRun) {
            operationsCompletionMap.put(operation, true);
            return false;
        }

        // if this operation has not started yet
        if (!isStabilizedStarted) return true;

        // if it is running check if it is stabilized, if so mark as complete
        if (isStabilized(model, context)) operationsCompletionMap.put(operation, true);
        return false;
    }

    /**
     * Checks if the update request is complete by retrieving the operation statuses in
     * {@link CallbackContext#getOperationsStabilizationMap()}
     * @param context {@link CallbackContext}
     * @return boolean indicates whether the update is done
     */
    public static boolean isUpdateStabilized(final CallbackContext context) {
        for (Map.Entry<UpdateOperations, Boolean> entry : context.getOperationsStabilizationMap().entrySet()) {
            if (!entry.getValue()) return false;
        }
        return true;
    }

    /**
     * Checks if previous {@link UpdateOperations} is complete
     * to avoid running other operations until previous operation is done
     * @param context {@link CallbackContext}
     * @param previousOperation {@link UpdateOperations}
     * @return boolean indicates whether the previous operation is done
     */
    public static boolean isPreviousOperationDone(final CallbackContext context,
                                                  final UpdateOperations previousOperation) {
        // Checks if previous operation is done. If no previous operation is running, mark as done
        return previousOperation == null ?
                true : context.getOperationsStabilizationMap().get(previousOperation);
    }
}
