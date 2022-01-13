package software.amazon.cloudformation.stackset.util;

import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.cloudformation.stackset.ManagedExecution;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

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
     * @param previousTags  previous resource and stack tags
     * @param desiredTags   desired resource and stack tags
     * @return
     */
    public static boolean isStackSetConfigEquals(final ResourceModel previousModel,
                                                 final ResourceModel desiredModel,
                                                 final Map<String, String> previousTags,
                                                 final Map<String, String> desiredTags) {

        if (!equals(previousTags, desiredTags))
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

        if (!equals(previousModel.getParameters(), desiredModel.getParameters()))
            return false;

        if (!equals(previousModel.getAutoDeployment(), desiredModel.getAutoDeployment()))
            return false;

        // If TemplateURL is specified, always call Update API, Service client will decide if it is updatable
        return desiredModel.getTemplateURL() == null;
    }

    /**
     * Compares if desired model uses the same ManagedExecution
     * when it comes to updating the resource
     *
     * @param previousManagedExecution previous {@link ManagedExecution}
     * @param desiredManagedExecution desired {@link ManagedExecution}
     * @return
     */
    public static boolean isStackSetConfigEquals(final ManagedExecution previousManagedExecution,
                                                 final ManagedExecution desiredManagedExecution) {
        final ManagedExecution defaultConfig = ManagedExecution.builder().active(false).build();
        final ManagedExecution previousConfig = (Objects.isNull(previousManagedExecution) || Objects.isNull(previousManagedExecution.getActive()))
                ? defaultConfig : previousManagedExecution;
        final ManagedExecution currentConfig = (Objects.isNull(desiredManagedExecution) || Objects.isNull(desiredManagedExecution.getActive()))
                ? defaultConfig : desiredManagedExecution;
        return equals(currentConfig, previousConfig);
    }

    /**
     * Compares if two collections equal in a null-safe way.
     *
     * @param collection1
     * @param collection2
     * @return boolean indicates if two collections equal.
     */
    public static boolean equals(final Collection<?> collection1, final Collection<?> collection2) {
        boolean equals = false;
        if (collection1 != null && collection2 != null) {
            equals = collection1.size() == collection2.size()
                    && collection1.containsAll(collection2) && collection2.containsAll(collection1);
        } else if (collection1 == null && collection2 == null) {
            equals = true;
        }
        return equals;
    }

    /**
     * Compares if two objects equal in a null-safe way.
     *
     * @param object1
     * @param object2
     * @return boolean indicates if two objects equal.
     */
    public static boolean equals(final Object object1, final Object object2) {
        boolean equals = false;
        if (object1 != null && object2 != null) {
            equals = object1.equals(object2);
        } else if (object1 == null && object2 == null) {
            equals = true;
        }
        return equals;
    }

    public static boolean isSelfManaged(final ResourceModel model) {
        return PermissionModels.SELF_MANAGED.equals(PermissionModels.fromValue(model.getPermissionModel()));
    }
}
