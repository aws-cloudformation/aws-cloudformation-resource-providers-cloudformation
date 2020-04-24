package software.amazon.cloudformation.stackset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;
import software.amazon.cloudformation.stackset.util.EnumUtils.Operations;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

@Data
@Builder
@JsonDeserialize(builder = CallbackContext.CallbackContextBuilder.class)
public class CallbackContext {

    // Operation Id to verify stabilization for StackSet operation.
    private String operationId;

    // Indicates initiation of analyzing template.
    private boolean templateAnalyzed;

    // Indicates initiation of resource stabilization.
    private boolean stackSetCreated;

    // Indicates initiation of stack instances creation.
    private boolean addStacksInstancesStarted;

    // Indicates initiation of stack instances delete.
    private boolean deleteStacksInstancesStarted;

    // Indicates initiation of stack set update.
    private boolean updateStackSetStarted;

    // Indicates initiation of stack instances update.
    private boolean updateStacksInstancesStarted;

    // Total running time
    @Builder.Default
    private int elapsedTime = 0;

    private StackInstances stackInstancesInOperation;

    // List to keep track on the complete status for creating
    @Builder.Default
    private Queue<StackInstances> createStacksInstancesQueue = new LinkedList<>();

    // List to keep track on stack instances for deleting
    @Builder.Default
    private Queue<StackInstances> deleteStacksInstancesQueue = new LinkedList<>();

    // List to keep track on stack instances for update
    @Builder.Default
    private Queue<StackInstances> updateStacksInstancesQueue = new LinkedList<>();

    /**
     * Default as 0, will be {@link software.amazon.cloudformation.stackset.util.Stabilizer#BASE_CALLBACK_DELAY_SECONDS}
     * When it enters the first IN_PROGRESS callback
     */
    @Builder.Default private int currentDelaySeconds = 0;

    // Map to keep track on the complete status for operations in Update
    @Builder.Default
    private Map<Operations, Boolean> operationsStabilizationMap = Arrays.stream(Operations.values())
            .collect(Collectors.toMap(e -> e, e -> false));

    /**
     * Increments {@link CallbackContext#elapsedTime} and returns the total elapsed time
     * @return {@link CallbackContext#getElapsedTime()} after incrementing
     */
    @JsonIgnore
    public int incrementElapsedTime() {
        elapsedTime = elapsedTime + currentDelaySeconds;
        return elapsedTime;
    }
}
