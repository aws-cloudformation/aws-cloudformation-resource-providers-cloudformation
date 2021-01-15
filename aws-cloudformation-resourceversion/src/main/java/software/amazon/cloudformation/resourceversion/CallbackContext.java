package software.amazon.cloudformation.resourceversion;

import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.Builder
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class CallbackContext extends StdCallbackContext {
    private String registrationToken;
    private DeprecatedStatus deprecatedStatus;
}
