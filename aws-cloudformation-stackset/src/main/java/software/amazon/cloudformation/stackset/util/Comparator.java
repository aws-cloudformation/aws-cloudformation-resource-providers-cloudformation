package software.amazon.cloudformation.stackset.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.Collection;

/**
 * Utility class to help comparing previous model and desire model
 */
public class Comparator {

    /**
     * Compares if desired model uses the same stack set configs other than stack instances
     * when it comes to updating the resource
     *
     * @param previousModel previous {@link ResourceModel}
     * @param desiredModel  desired {@link ResourceModel}
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
        return desiredModel.getTemplateBody() != null || desiredModel.getTemplateURL() == null;
    }

    /**
     * Compares if two collections equal in a null-safe way.
     *
     * @param collection1
     * @param collection2
     * @return boolean indicates if two collections equal.
     */
    public static boolean isEquals(final Collection<?> collection1, final Collection<?> collection2) {
        if (collection1 == null) return collection2 == null;
        return CollectionUtils.isEqualCollection(collection1, collection2);
    }

    public static boolean isSelfManaged(final ResourceModel model) {
        return PermissionModels.fromValue(model.getPermissionModel()).equals(PermissionModels.SELF_MANAGED);
    }
}
