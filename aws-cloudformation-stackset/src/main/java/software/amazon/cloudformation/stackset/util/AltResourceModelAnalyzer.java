package software.amazon.cloudformation.stackset.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

@Builder
@Data
public class AltResourceModelAnalyzer {

    private ResourceModel previousModel;
    private ResourceModel currentModel;

    private final Set<StackInstances> stackInstancesToDelete = new HashSet<>();
    private final Set<StackInstances> stackInstancesToCreate = new HashSet<>();
    private final Set<StackInstances> stackInstancesToUpdate = new HashSet<>();

    /*
    * This class takes previous and current models to analyze the StackInstances to create, delete, and update
    * The process logic:
    * 1. For the regions only appear in previous model -- all the associated instances should be deleted
    * 2. For the regions only appear in current model -- all the associated instances should be created
    * 3. Further comparison will be done for the regions shared in two models by calling AltStackInstancesCalculator
    * */
    public void analyze(final StackInstancesPlaceHolder placeHolder) {
        Set<StackInstances> previousStackInstancesGroup = previousModel == null ?
                new HashSet<>() : previousModel.getStackInstancesGroup();
        Set<StackInstances> currentStackInstancesGroup = currentModel == null ?
                new HashSet<>() : currentModel.getStackInstancesGroup();

        Validator.validateServiceMangedInstancesGroup(previousStackInstancesGroup);
        Validator.validateServiceMangedInstancesGroup(currentStackInstancesGroup);

        HashMap<String, Set<StackInstances>> previousStackInstancesByRegion = regroupStackInstancesByRegion(previousStackInstancesGroup);
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = regroupStackInstancesByRegion(currentStackInstancesGroup);

        Set<String> previousRegions = previousStackInstancesByRegion.keySet();
        Set<String> currentRegions = currentStackInstancesByRegion.keySet();

        setDiff(previousRegions, currentRegions).forEach(
                region -> stackInstancesToDelete.addAll(previousStackInstancesByRegion.get(region))
        );

        setDiff(currentRegions, previousRegions).forEach(
                region -> stackInstancesToCreate.addAll(currentStackInstancesByRegion.get(region))
        );

        setInter(currentRegions, previousRegions).forEach(
                region -> new AltStackInstancesCalculator(region,
                        previousStackInstancesByRegion.get(region),
                        currentStackInstancesByRegion.get(region))
                        .calculate(
                                stackInstancesToDelete,
                                stackInstancesToCreate,
                                stackInstancesToUpdate)
        );

        placeHolder.setCreateStackInstances(new ArrayList<>(stackInstancesToCreate));
        placeHolder.setDeleteStackInstances(new ArrayList<>(stackInstancesToDelete));
        placeHolder.setUpdateStackInstances(new ArrayList<>(stackInstancesToUpdate));
    }

    private static HashMap<String, Set<StackInstances>> regroupStackInstancesByRegion (final Collection<StackInstances> stackInstancesGroup) {
        HashMap<String, Set<StackInstances>> stackInstancesGroupsByRegion = new HashMap<>();
        if (CollectionUtils.isNullOrEmpty(stackInstancesGroup)) return stackInstancesGroupsByRegion;

        for (final StackInstances stackInstances : stackInstancesGroup) {
            for (final String region : stackInstances.getRegions()) {
                if (!stackInstancesGroupsByRegion.containsKey(region)) {
                    stackInstancesGroupsByRegion.put(region, new HashSet<>());
                }
                stackInstancesGroupsByRegion.get(region).add(StackInstances.builder()
                        .regions(new HashSet<>(Collections.singletonList(region)))
                        .deploymentTargets(stackInstances.getDeploymentTargets())
                        .parameterOverrides(stackInstances.getParameterOverrides())
                        .build());
            }
        }
        return stackInstancesGroupsByRegion;
    }

    public static Set<String> setDiff(Set<String> A, Set<String> B) {
        Set<String> resultSet = new HashSet<>(A);
        resultSet.removeAll(B);
        return resultSet;
    }

    public static Set<String> setInter(Set<String> A, Set<String> B) {
        Set<String> resultSet = new HashSet<>(A);
        resultSet.retainAll(B);
        return resultSet;
    }

    public static Set<String> setUnion(Set<String> A, Set<String> B) {
        Set<String> resultSet = new HashSet<>(A);
        resultSet.addAll(B);
        return resultSet;
    }
}
