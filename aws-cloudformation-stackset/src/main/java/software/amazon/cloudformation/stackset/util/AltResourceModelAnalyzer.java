package software.amazon.cloudformation.stackset.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.stackset.Parameter;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

@Builder
@Data
public class AltResourceModelAnalyzer {

    private ResourceModel previousModel;
    private ResourceModel currentModel;

    private final Set<StackInstances> stackInstancesToDelete = new LinkedHashSet<>();
    private final Set<StackInstances> stackInstancesToCreate = new LinkedHashSet<>();
    private final Set<StackInstances> stackInstancesToUpdate = new LinkedHashSet<>();

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

        Set<String> previousModelRegionOrder = new LinkedHashSet<>();
        Set<String> currentModelRegionOrder = new LinkedHashSet<>();

        if (currentModel != null && currentModel.getOperationPreferences() != null && currentModel.getOperationPreferences().getRegionOrder() != null)
            currentModelRegionOrder.addAll(currentModel.getOperationPreferences().getRegionOrder());
        if (previousModel != null && previousModel.getOperationPreferences() != null && previousModel.getOperationPreferences().getRegionOrder() != null)
            previousModelRegionOrder.addAll(previousModel.getOperationPreferences().getRegionOrder());

        HashMap<String, Set<StackInstances>> previousStackInstancesByRegion = regroupStackInstancesByRegion(previousStackInstancesGroup, previousModelRegionOrder);
        HashMap<String, Set<StackInstances>> currentStackInstancesByRegion = regroupStackInstancesByRegion(currentStackInstancesGroup, currentModelRegionOrder);

        Set<String> previousRegions = previousStackInstancesByRegion.keySet();
        Set<String> currentRegions = currentStackInstancesByRegion.keySet();

        setDiff(previousRegions, currentRegions).forEach(
                region -> stackInstancesToDelete.addAll(previousStackInstancesByRegion.get(region))
        );

        setDiff(currentRegions, previousRegions).forEach(
                region -> stackInstancesToCreate.addAll(currentStackInstancesByRegion.get(region))
        );

        HashMap<String, Set<Parameter>> ouDeploymentParametersMap = findDeploymentParametersForOUs(currentStackInstancesByRegion);

        setInter(currentRegions, previousRegions).forEach(
                region -> new AltStackInstancesCalculator(region,
                        previousStackInstancesByRegion.get(region),
                        currentStackInstancesByRegion.get(region))
                        .calculate(
                                stackInstancesToDelete,
                                stackInstancesToCreate,
                                stackInstancesToUpdate,
                                ouDeploymentParametersMap)
        );

        placeHolder.setCreateStackInstances(new ArrayList<>(stackInstancesToCreate));
        placeHolder.setDeleteStackInstances(new ArrayList<>(stackInstancesToDelete));
        placeHolder.setUpdateStackInstances(new ArrayList<>(stackInstancesToUpdate));
    }

    /*
     *  If an OU is associated with different parameter sets, will raise an error.
     *  1. This is the original process logic when ALT is not enabled.
     *  2. Although users logically CAN associate an OU with different with ALT filter, but we cannot check if the input is valid
     *  2.1 For example, (OU - account1) and (OU - account2). It's up to OU's structure if these two targets can be
     *      associated with two parameters -- if OU is set(account1, account2, account3), then not valid.
     *  2.2 So we chose to raise and error to align with previous implementation and avoid possible ambiguity
     * */

    private static HashMap<String, Set<Parameter>> findDeploymentParametersForOUs(final HashMap<String, Set<StackInstances>> currentStackInstancesByRegion) {
        HashMap<String, Set<Parameter>> ouDeploymentParameters = new HashMap<>();

        for (final String region : currentStackInstancesByRegion.keySet()) {
            for (final StackInstances stackInstances : currentStackInstancesByRegion.get(region)) {
                Set<Parameter> parameters = stackInstances.getParameterOverrides();

                stackInstances.getDeploymentTargets().getOrganizationalUnitIds().forEach(
                        ou -> {
                            if (ouDeploymentParameters.containsKey(ou) && ouDeploymentParameters.get(ou) != parameters) {
                                throw new CfnInvalidRequestException("An OrganizationalUnitIds cannot be associated with more than one Parameters set");
                            }
                            ouDeploymentParameters.put(ou, parameters);
                        }
                );
            }
        }
        return ouDeploymentParameters;
    }

    private static HashMap<String, Set<StackInstances>> regroupStackInstancesByRegion (final Collection<StackInstances> stackInstancesGroup, final Set<String> regionOrder) {
        HashMap<String, Set<StackInstances>> stackInstancesGroupsByRegion = new LinkedHashMap<>();
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
        return regroupStackInstancesByRegionOrder(stackInstancesGroupsByRegion, regionOrder);
    }

    private static HashMap<String, Set<StackInstances>> regroupStackInstancesByRegionOrder(
            final HashMap<String, Set<StackInstances>> stackInstancesGroupsByRegion,
            final Set<String> regionOrder) {

        if(regionOrder.size() == 0) return stackInstancesGroupsByRegion;
        HashMap<String, Set<StackInstances>> stackInstancesGroupsByRegionOrder = new LinkedHashMap<>();
        for(String region: regionOrder) {
            if(stackInstancesGroupsByRegion.containsKey(region)) {
                stackInstancesGroupsByRegionOrder.put(region, stackInstancesGroupsByRegion.get(region));
            }
        }
        stackInstancesGroupsByRegion.entrySet().stream()
                .filter(entry -> !stackInstancesGroupsByRegionOrder.containsKey(entry.getKey()))
                .forEach(entry -> stackInstancesGroupsByRegionOrder.put(entry.getKey(), entry.getValue()));

        return stackInstancesGroupsByRegionOrder;
    }

    public static Set<String> setDiff(Set<String> A, Set<String> B) {
        Set<String> resultSet = new LinkedHashSet<>(A);
        resultSet.removeAll(B);
        return resultSet;
    }

    public static Set<String> setInter(Set<String> A, Set<String> B) {
        Set<String> resultSet = new LinkedHashSet<>(A);
        resultSet.retainAll(B);
        return resultSet;
    }

    public static Set<String> setUnion(Set<String> A, Set<String> B) {
        Set<String> resultSet = new LinkedHashSet<>(A);
        resultSet.addAll(B);
        return resultSet;
    }
}
