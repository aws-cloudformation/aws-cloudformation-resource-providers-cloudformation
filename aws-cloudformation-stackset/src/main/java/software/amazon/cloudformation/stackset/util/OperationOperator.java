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
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetResponse;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.CfnNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.HashSet;
import java.util.Set;

import static software.amazon.cloudformation.stackset.translator.RequestTranslator.createStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.deleteStackInstancesRequest;
import static software.amazon.cloudformation.stackset.translator.RequestTranslator.describeStackSetRequest;
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
     * Performs to delete stack instances based on the new removed regions
     * with all targets including new removed targets
     * @param regionsToDelete Region to delete
     * @return {@link DeleteStackInstancesResponse#operationId()}
     */
    private String deleteStackInstancesByRegions(final Set<String> regionsToDelete) {
        final DeleteStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                deleteStackInstancesRequest(previousModel.getStackSetId(), desiredModel.getOperationPreferences(),
                        previousModel.getDeploymentTargets(), regionsToDelete), client::deleteStackInstances);

        context.setDeleteStacksByRegionsStarted(true);
        return response.operationId();
    }

    /**
     * Performs to delete stack instances based on the newly removed targets
     * @param regionsDeleted Region have been delete in {@link OperationOperator#deleteStackInstancesByRegions}
     * @param targetsToDelete Targets to delete
     * @return {@link DeleteStackInstancesResponse#operationId()}
     */
    private String deleteStackInstancesByTargets(final Set<String> regionsDeleted, final Set<String> targetsToDelete) {
        // Constructing deploymentTargets which need to be deleted
        final boolean isSelfManaged = PermissionModels.SELF_MANAGED
                .equals(PermissionModels.fromValue(previousModel.getPermissionModel()));
        final DeploymentTargets deploymentTargets = DeploymentTargets.builder().build();

        if (isSelfManaged) {
            deploymentTargets.setAccounts(targetsToDelete);
        } else {
            deploymentTargets.setOrganizationalUnitIds(targetsToDelete);
        }

        final Set<String> regionsToDelete = new HashSet<>(previousModel.getRegions());

        // Avoid to delete regions that were already deleted above
        if (!regionsDeleted.isEmpty()) regionsToDelete.removeAll(regionsDeleted);

        final DeleteStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                deleteStackInstancesRequest(previousModel.getStackSetId(), desiredModel.getOperationPreferences(),
                        deploymentTargets, regionsToDelete), client::deleteStackInstances);

        context.setDeleteStacksByTargetsStarted(true);
        return response.operationId();
    }

    /**
     * Performs to create stack instances based on the new added regions
     * with all targets including new added targets
     * @param regionsToAdd Region to add
     * @return {@link CreateStackInstancesResponse#operationId()}
     */
    private String addStackInstancesByRegions(final Set<String> regionsToAdd) {
        final CreateStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(
                createStackInstancesRequest(desiredModel.getStackSetId(), desiredModel.getOperationPreferences(),
                        desiredModel.getDeploymentTargets(), regionsToAdd),
                client::createStackInstances);

        context.setAddStacksByRegionsStarted(true);
        return response.operationId();
    }

    /**
     * Performs to create stack instances based on the new added targets
     * @param regionsAdded Region have been added in {@link OperationOperator#addStackInstancesByRegions}
     * @param targetsToAdd Targets to add
     * @return {@link CreateStackInstancesResponse#operationId()}
     */
    private String addStackInstancesByTargets(final Set<String> regionsAdded, final Set<String> targetsToAdd) {
        // Constructing deploymentTargets which need to be added
        final boolean isSelfManaged = PermissionModels.SELF_MANAGED
                .equals(PermissionModels.fromValue(desiredModel.getPermissionModel()));
        final DeploymentTargets deploymentTargets = DeploymentTargets.builder().build();

        if (isSelfManaged) {
            deploymentTargets.setAccounts(targetsToAdd);
        } else {
            deploymentTargets.setOrganizationalUnitIds(targetsToAdd);
        }

        final Set<String> regionsToAdd = new HashSet<>(desiredModel.getRegions());
        /**
         * Avoid to create instances in regions that have already created in
         * {@link OperationOperator#addStackInstancesByRegions}
         */
        if (!regionsAdded.isEmpty()) regionsToAdd.removeAll(regionsAdded);

        final CreateStackInstancesResponse response = proxy.injectCredentialsAndInvokeV2(createStackInstancesRequest(
                desiredModel.getStackSetId(), desiredModel.getOperationPreferences(), deploymentTargets, regionsToAdd),
                client::createStackInstances);

        context.setAddStacksByTargetsStarted(true);
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
     * Update the StackSet with the {@link EnumUtils.UpdateOperations} passed in
     * @param operation {@link EnumUtils.UpdateOperations}
     * @param regions Regions to add or delete
     * @param targets Targets to add or delete
     */
    public void updateStackSet(
            final EnumUtils.UpdateOperations operation,
            final Set<String> regions,
            final Set<String> targets) {

        try {
            String operationId = null;
            switch (operation) {
                case STACK_SET_CONFIGS:
                    operationId = updateStackSetConfig();
                    break;
                case DELETE_INSTANCES_BY_REGIONS:
                    operationId = deleteStackInstancesByRegions(regions);
                    break;
                case DELETE_INSTANCES_BY_TARGETS:
                    operationId = deleteStackInstancesByTargets(regions, targets);
                    break;
                case ADD_INSTANCES_BY_REGIONS:
                    operationId = addStackInstancesByRegions(regions);
                    break;
                case ADD_INSTANCES_BY_TARGETS:
                    operationId = addStackInstancesByTargets(regions, targets);
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
            context.incrementRetryCounter();
        }
    }
}
