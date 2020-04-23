package software.amazon.cloudformation.stackset.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.InvalidOperationException;
import software.amazon.awssdk.services.cloudformation.model.OperationInProgressException;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.awssdk.services.cloudformation.model.StackSetNotFoundException;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackSetRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.updateStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.updateStackSetRequest;

/**
 * Helper class to perform operations that we need to interact with service client from the requests
 */
@AllArgsConstructor
@Builder
public class OperationOperator {

    private AmazonWebServicesClientProxy proxy;
    private CloudFormationClient client;
    private ResourceModel previousModel;
    private ResourceModel desiredModel;
    private Logger logger;
    private CallbackContext context;

    private static String OPERATION_IN_PROGRESS_MSG = "StackSet Operation retrying due to prior operation incomplete";

    /**
     * Performs to update stack set configs
     * @return {@link UpdateStackSetResponse#operationId()}
     */
    private String updateStackSetConfig() {
        final UpdateStackSetResponse response = proxy.injectCredentialsAndInvokeV2(
                updateStackSetRequest(desiredModel), client::updateStackSet);

        context.setUpdateStackSetStarted(true);
        return response.operationId();
    }

    /**
     * Get {@link StackSet} from service client using stackSetId
     * @param stackSetId StackSet Id
     * @return {@link StackSet}
     */
    public StackSet getStackSet(final String stackSetId) {
        try {
            final DescribeStackSetResponse stackSetResponse = proxy.injectCredentialsAndInvokeV2(
                    describeStackSetRequest(stackSetId), client::describeStackSet);
            return stackSetResponse.stackSet();
        } catch (final StackSetNotFoundException e) {
            throw new CfnNotFoundException(e);
        }
    }

    /**
     * Invokes CreateStackInstances API to add new {@link StackInstances} based on {@link CallbackContext#getCreateStacksQueue()}
     * @return Operation Id from {@link CreateStackInstancesResponse}
     */
    private String addStackInstances() {
        final Queue<StackInstances> instancesQueue = context.getCreateStacksQueue();
        final CreateStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                createStackInstancesRequest(desiredModel.getStackSetId(), desiredModel.getOperationPreferences(),
                        instancesQueue.peek()), client::createStackInstances);
        context.setAddStacksStarted(true);
        // We remove the stack instances from queue Only if API invocation succeeds
        context.setStackInstancesInOperation(instancesQueue.remove());
        return response.operationId();
    }

    /**
     * Invokes DeleteStackInstances API to delete old {@link StackInstances} based on {@link CallbackContext#getDeleteStacksQueue()}
     * @return Operation Id from {@link DeleteStackInstancesResponse}
     */
    private String deleteStackInstances() {
        final Queue<StackInstances> instancesQueue = context.getDeleteStacksQueue();
        final DeleteStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                deleteStackInstancesRequest(desiredModel.getStackSetId(), desiredModel.getOperationPreferences(),
                        instancesQueue.peek()), client::deleteStackInstances);
        context.setDeleteStacksStarted(true);
        // We remove the stack instances from queue Only if API invocation succeeds
        context.setStackInstancesInOperation(instancesQueue.remove());
        return response.operationId();
    }

    /**
     * Invokes UpdateStackInstances API to update existing {@link StackInstances} based on {@link CallbackContext#getUpdateStacksQueue()}
     * @return Operation Id from {@link UpdateStackInstancesResponse}
     */
    private String updateStackInstances() {
        final Queue<StackInstances> instancesQueue = context.getUpdateStacksQueue();
        final UpdateStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                updateStackInstancesRequest(desiredModel.getStackSetId(), desiredModel.getOperationPreferences(),
                        instancesQueue.peek()), client::updateStackInstances);
        context.setUpdateStacksStarted(true);
        // We remove the stack instances from queue Only if API invocation succeeds
        context.setStackInstancesInOperation(instancesQueue.remove());
        return response.operationId();
    }

    /**
     * Update the StackSet with the {@link EnumUtils.Operations} passed in
     * @param operation {@link EnumUtils.Operations}
     */
    public void updateStackSet(final EnumUtils.Operations operation) {
        try {
            String operationId = null;
            switch (operation) {
                case STACK_SET_CONFIGS:
                    operationId = updateStackSetConfig();
                    break;
                case DELETE_INSTANCES:
                    operationId = deleteStackInstances();
                    break;
                case ADD_INSTANCES:
                    operationId = addStackInstances();
                    break;
                case UPDATE_INSTANCES:
                    operationId = updateStackInstances();
            }

            logger.log(String.format("%s [%s] %s update initiated",
                    ResourceModel.TYPE_NAME, desiredModel.getStackSetId(), operation));
            context.setOperationId(operationId);

        } catch (final InvalidOperationException e) {
            throw new CfnInvalidRequestException(e);

        } catch (final StackSetNotFoundException e) {
            throw new CfnNotFoundException(e);

        } catch (final OperationInProgressException e) {
            logger.log(OPERATION_IN_PROGRESS_MSG);
        }
    }
}
