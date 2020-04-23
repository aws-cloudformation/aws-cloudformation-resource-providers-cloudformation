package software.amazon.cloudformation.stackset.util;

public class EnumUtils {

    /**
     * Operations that need to complete during update
     */
    public enum Operations {
        STACK_SET_CONFIGS, ADD_INSTANCES, DELETE_INSTANCES, UPDATE_INSTANCES
    }

}
