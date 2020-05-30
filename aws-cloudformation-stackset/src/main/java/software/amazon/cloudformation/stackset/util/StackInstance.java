package software.amazon.cloudformation.stackset.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.amazon.cloudformation.stackset.Parameter;

import java.util.Set;

@Data
@Builder
@EqualsAndHashCode
public class StackInstance {

    @JsonProperty("Region")
    private String region;

    @JsonProperty("DeploymentTarget")
    private String deploymentTarget;

    @EqualsAndHashCode.Exclude
    private Set<Parameter> parameters;
}
