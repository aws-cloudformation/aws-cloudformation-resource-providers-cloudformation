package software.amazon.cloudformation.stackset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import software.amazon.cloudformation.stackset.util.EnumUtils.UpdateOperations;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@JsonDeserialize(builder = CallbackContext.CallbackContextBuilder.class)
public class CallbackContext {

    // Operation Id to verify stabilization for StackSet operation.
    private String operationId;

    // Elapsed counts of retries on specific exceptions.
    private int retries;

    // Indicates initiation of resource stabilization.
    private boolean stabilizationStarted;

    // Indicates initiation of stack instances creation.
    private boolean addStacksByRegionsStarted;

    // Indicates initiation of stack instances creation.
    private boolean addStacksByTargetsStarted;

    // Indicates initiation of stack instances delete.
    private boolean deleteStacksByRegionsStarted;

    // Indicates initiation of stack instances delete.
    private boolean deleteStacksByTargetsStarted;

    // Indicates initiation of stack set update.
    private boolean updateStackSetStarted;

    // Indicates initiation of stack instances update.
    private boolean updateStackInstancesStarted;

    // Total running time
    @Builder.Default
    private int elapsedTime = 0;

    /**
     * Default as 0, will be {@link software.amazon.cloudformation.stackset.util.Stabilizer#BASE_CALLBACK_DELAY_SECONDS}
     * When it enters the first IN_PROGRESS callback
     */
    @Builder.Default private int currentDelaySeconds = 0;

    // Map to keep track on the complete status for operations in Update
    @Builder.Default
    private Map<UpdateOperations, Boolean> operationsStabilizationMap = Arrays.stream(UpdateOperations.values())
            .collect(Collectors.toMap(e -> e, e -> false));

    @JsonIgnore
    public void incrementRetryCounter() {
        retries++;
    }

    /**
     * Increments {@link CallbackContext#elapsedTime} and returns the total elapsed time
     * @return {@link CallbackContext#getElapsedTime()} after incrementing
     */
    @JsonIgnore
    public int incrementElapsedTime() {
        elapsedTime = elapsedTime + currentDelaySeconds;
        return elapsedTime;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class CallbackContextBuilder {
    }
}
