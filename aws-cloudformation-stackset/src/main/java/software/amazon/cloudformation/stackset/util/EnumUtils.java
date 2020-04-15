package software.amazon.cloudformation.stackset.util;

public class EnumUtils {

    /**
     * Operations that need to complete during update
     */
    public enum UpdateOperations {
        STACK_SET_CONFIGS, ADD_INSTANCES_BY_REGIONS, ADD_INSTANCES_BY_TARGETS,
        DELETE_INSTANCES_BY_REGIONS,DELETE_INSTANCES_BY_TARGETS
    }

}
