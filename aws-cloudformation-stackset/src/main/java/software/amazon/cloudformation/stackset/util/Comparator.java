package software.amazon.cloudformation.stackset.util;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.Collection;

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

        if (StringUtils.compare(previousModel.getTemplateBody(), desiredModel.getTemplateBody()) != 0)
            return false;

        // If TemplateURL is specified, always call Update API, Service client will decide if it is updatable
        if (desiredModel.getTemplateBody() == null && desiredModel.getTemplateURL() != null)
            return false;

        return true;
    }

    /**
     * Checks if stack instances need to be updated
     * @param context {@link CallbackContext}
     * @return
     */
    public static boolean isUpdatingStackInstances(final CallbackContext context) {
        // If no stack instances need to be updated
        if (context.getUpdateStackInstancesQueue().isEmpty() && !context.isUpdateStackInstancesStarted()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if there is any stack instances need to be delete during the update
     * @param context {@link CallbackContext}
     * @return
     */
    public static boolean isDeletingStackInstances(final CallbackContext context) {
        // If no stack instances need to be deleted
        if (context.getDeleteStackInstancesQueue().isEmpty() && !context.isDeleteStackInstancesStarted()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if new stack instances need to be added
     * @param context {@link CallbackContext}
     * @return
     */
    public static boolean isAddingStackInstances(final CallbackContext context) {
        // If no stack instances need to be added
        if (context.getCreateStackInstancesQueue().isEmpty() && !context.isAddStackInstancesStarted()) {
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
        return collection1.equals(collection2);
    }

    public static boolean isSelfManaged(final ResourceModel model) {
        return PermissionModels.fromValue(model.getPermissionModel()).equals(PermissionModels.SELF_MANAGED);
    }
}
