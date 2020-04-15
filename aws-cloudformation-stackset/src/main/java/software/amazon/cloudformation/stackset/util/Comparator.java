package software.amazon.cloudformation.stackset.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.Collection;
import java.util.Set;

import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.ADD_INSTANCES_BY_REGIONS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.ADD_INSTANCES_BY_TARGETS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.DELETE_INSTANCES_BY_REGIONS;
import static software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations.DELETE_INSTANCES_BY_TARGETS;

/**
 * Utility class to help comparing previous model and desire model
 */
public class Comparator {

    /**
     * Compares if desired model uses the same stack set configs other than stack instances
     * when it comes to updating the resource
     * @param previousModel previous {@link ResourceModel}
     * @param desiredModel desired {@link ResourceModel}
     * @return
     */
    public static boolean isStackSetConfigEquals(
            final ResourceModel previousModel, final ResourceModel desiredModel) {

        if (!isEquals(previousModel.getTags(), desiredModel.getTags()))
            return false;

        if (StringUtils.compare(previousModel.getAdministrationRoleARN(),
                desiredModel.getAdministrationRoleARN()) != 0)
            return false;

        if (StringUtils.compare(previousModel.getDescription(), desiredModel.getDescription()) != 0)
            return false;

        if (StringUtils.compare(previousModel.getExecutionRoleName(), desiredModel.getExecutionRoleName()) != 0)
            return false;

        if (StringUtils.compare(previousModel.getTemplateURL(), desiredModel.getTemplateURL()) != 0)
            return false;

        if (StringUtils.compare(previousModel.getTemplateBody(), desiredModel.getTemplateBody()) != 0)
            return false;

        return true;
    }

    /**
     * Checks if stack instances need to be updated
     * @param previousModel previous {@link ResourceModel}
     * @param desiredModel desired {@link ResourceModel}
     * @param context {@link CallbackContext}
     * @return
     */
    public static boolean isUpdatingStackInstances(
            final ResourceModel previousModel,
            final ResourceModel desiredModel,
            final CallbackContext context) {

        // if updating stack instances is unnecessary, mark all instances operation as complete
        if (CollectionUtils.isEqualCollection(previousModel.getRegions(), desiredModel.getRegions()) &&
                previousModel.getDeploymentTargets().equals(desiredModel.getDeploymentTargets())) {

            context.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);
            context.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_TARGETS, true);
            context.getOperationsStabilizationMap().put(ADD_INSTANCES_BY_REGIONS, true);
            context.getOperationsStabilizationMap().put(ADD_INSTANCES_BY_TARGETS, true);
            return false;
        }
        return true;
    }

    /**
     * Checks if there is any stack instances need to be delete during the update
     * @param regionsToDelete regions to delete
     * @param targetsToDelete targets (accounts or OUIDs) to delete
     * @param context {@link CallbackContext}
     * @return
     */
    public static boolean isDeletingStackInstances(
            final Set<String> regionsToDelete,
            final Set<String> targetsToDelete,
            final CallbackContext context) {

        // If no stack instances need to be deleted, mark DELETE_INSTANCES operations as done.
        if (regionsToDelete.isEmpty() && targetsToDelete.isEmpty()) {
            context.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_REGIONS, true);
            context.getOperationsStabilizationMap().put(DELETE_INSTANCES_BY_TARGETS, true);
            return false;
        }
        return true;
    }

    /**
     * Checks if new stack instances need to be added
     * @param regionsToAdd regions to add
     * @param targetsToAdd targets to add
     * @param context {@link CallbackContext}
     * @return
     */
    public static boolean isAddingStackInstances(
            final Set<String> regionsToAdd,
            final Set<String> targetsToAdd,
            final CallbackContext context) {

        // If no stack instances need to be added, mark ADD_INSTANCES operations as done.
        if (regionsToAdd.isEmpty() && targetsToAdd.isEmpty()) {
            context.getOperationsStabilizationMap().put(ADD_INSTANCES_BY_REGIONS, true);
            context.getOperationsStabilizationMap().put(ADD_INSTANCES_BY_TARGETS, true);
            return false;
        }
        return true;
    }

    /**
     * Compares if two collections equal in a null-safe way.
     * @param collection1
     * @param collection2
     * @return boolean indicates if two collections equal.
     */
    public static boolean isEquals(final Collection<?> collection1, final Collection<?> collection2) {
        if (collection1 == null) return collection2 == null ? true : false;
        return CollectionUtils.isEqualCollection(collection1, collection2);
    }
}
