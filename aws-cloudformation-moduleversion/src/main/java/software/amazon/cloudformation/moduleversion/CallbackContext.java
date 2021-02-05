package software.amazon.cloudformation.moduleversion;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import software.amazon.awssdk.services.cloudformation.model.DeprecatedStatus;
import software.amazon.cloudformation.proxy.StdCallbackContext;

import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString
public class CallbackContext extends StdCallbackContext {

    @Getter
    @Setter
    private DeprecatedStatus deprecatedStatus;

    @Getter
    @Setter
    private String registrationToken;

    @Getter
    private List<ResourceModel> modulesToList;

    @Getter
    @Setter
    private ResourceModel moduleToList;

    boolean hasModuleToList() {
        return this.getModulesToList() != null && !this.getModulesToList().isEmpty();
    }

    void addModulesToList(final List<ResourceModel> models) {
        if (this.getModulesToList() == null) {
            this.modulesToList = new LinkedList<>();
        }
        this.getModulesToList().addAll(models);
    }
}
