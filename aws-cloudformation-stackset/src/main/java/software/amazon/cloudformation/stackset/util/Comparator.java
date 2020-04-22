package software.amazon.cloudformation.stackset.util;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.Collection;

/**
 * Utility class to help comparing previous model and desire model
 */
public class Comparator {

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

    public static boolean isSelfManaged(final ResourceModel model) {
        return PermissionModels.fromValue(model.getPermissionModel()).equals(PermissionModels.SELF_MANAGED);
    }
}
