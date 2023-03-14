package software.amazon.cloudformation.stack;

import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.GetStackPolicyResponse;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
        private DescribeStacksResponse describeStacksResponse;
        private GetStackPolicyResponse getStackPolicyResponse;
}
