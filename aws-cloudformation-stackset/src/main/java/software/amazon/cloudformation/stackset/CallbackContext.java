package software.amazon.cloudformation.stackset;

import software.amazon.cloudformation.proxy.StdCallbackContext;

import java.util.LinkedList;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {

    // List to keep track on the complete status for creating
    private List<StackInstances> createStacksList = new LinkedList<>();

    // List to keep track on stack instances for deleting
    private List<StackInstances> deleteStacksList = new LinkedList<>();

    // List to keep track on stack instances for update
    private List<StackInstances> updateStacksList = new LinkedList<>();

}
