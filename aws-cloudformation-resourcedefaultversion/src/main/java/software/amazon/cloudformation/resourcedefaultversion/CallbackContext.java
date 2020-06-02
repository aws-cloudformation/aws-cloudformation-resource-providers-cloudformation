package software.amazon.cloudformation.resourcedefaultversion;

import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.Builder
public class CallbackContext extends StdCallbackContext {
}
