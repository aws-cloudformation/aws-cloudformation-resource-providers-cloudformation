package software.amazon.cloudformation.stackset.util;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.Parameter;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.cloudformation.stackset.util.Comparator.isSelfManaged;

/**
 * Utility class to hold {@link StackInstances} that need to be modified during the update
 */
@Builder
@Data
public class InstancesAnalyzer {

    private ResourceModel previousModel;

    private ResourceModel desiredModel;

    /**
     * Aggregates flat {@link StackInstance} to a group of {@link StackInstances} to call
     * corresponding StackSet APIs
     *
     * @param flatStackInstances {@link StackInstance}
     * @return {@link StackInstances} set
     */
    public static Set<StackInstances> aggregateStackInstances(
            @NonNull final Set<StackInstance> flatStackInstances, final boolean isSelfManaged) {
        final Set<StackInstances> groupedStacks = groupInstancesByTargets(flatStackInstances, isSelfManaged);
        return aggregateInstancesByRegions(groupedStacks, isSelfManaged);
    }

    /**
     * Aggregates flat {@link StackInstance} to a group of {@link StackInstances} to construct resource model
     * <p>Note:</p>
     * This is being used only because currently we can not retrieve OUs from CloudFormation DescribeStackInstances API
     * Hence, we are returning AccountIDs for stack instances.
     *
     * @param flatStackInstances {@link StackInstance}
     * @return {@link StackInstances} set
     */
    public static Set<StackInstances> aggregateStackInstancesForRead(@NonNull final Set<StackInstance> flatStackInstances) {
        final Set<StackInstances> groupedStacksInstances = groupInstancesByTargets(flatStackInstances, true);
        return aggregateInstancesByRegions(groupedStacksInstances, true);
    }

    /**
     * Group regions by {@link DeploymentTargets} and {@link StackInstance#getParameters()}
     *
     * @return {@link StackInstances}
     */
    private static Set<StackInstances> groupInstancesByTargets(
            final Set<StackInstance> flatStackInstances, final boolean isSelfManaged) {

        final Map<List<Object>, StackInstances> groupedStacksMap = new HashMap<>();
        for (final StackInstance stackInstance : flatStackInstances) {
            final String target = stackInstance.getDeploymentTarget();
            final String region = stackInstance.getRegion();
            final Set<Parameter> parameterSet = stackInstance.getParameters();
            final List<Object> compositeKey = Arrays.asList(target, parameterSet);

            if (groupedStacksMap.containsKey(compositeKey)) {
                groupedStacksMap.get(compositeKey).getRegions().add(stackInstance.getRegion());
            } else {
                final DeploymentTargets targets = DeploymentTargets.builder().build();
                if (isSelfManaged) {
                    targets.setAccounts(new HashSet<>(Arrays.asList(target)));
                } else {
                    targets.setOrganizationalUnitIds(new HashSet<>(Arrays.asList(target)));
                }

                final StackInstances stackInstances = StackInstances.builder()
                        .regions(new HashSet<>(Arrays.asList(region)))
                        .deploymentTargets(targets)
                        .parameterOverrides(parameterSet)
                        .build();
                groupedStacksMap.put(compositeKey, stackInstances);
            }
        }
        return new HashSet<>(groupedStacksMap.values());
    }

    /**
     * Aggregates instances with similar {@link StackInstances#getRegions()}
     *
     * @param groupedStacks {@link StackInstances} set
     * @return Aggregated {@link StackInstances} set
     */
    private static Set<StackInstances> aggregateInstancesByRegions(
            final Set<StackInstances> groupedStacks,
            final boolean isSelfManaged) {

        final Map<List<Object>, StackInstances> groupedStacksMap = new HashMap<>();
        for (final StackInstances stackInstances : groupedStacks) {
            final DeploymentTargets target = stackInstances.getDeploymentTargets();
            final Set<Parameter> parameterSet = stackInstances.getParameterOverrides();
            final List<Object> compositeKey = Arrays.asList(stackInstances.getRegions(), parameterSet);
            if (groupedStacksMap.containsKey(compositeKey)) {
                if (isSelfManaged) {
                    groupedStacksMap.get(compositeKey).getDeploymentTargets()
                            .getAccounts().addAll(target.getAccounts());
                } else {
                    groupedStacksMap.get(compositeKey).getDeploymentTargets()
                            .getOrganizationalUnitIds().addAll(target.getOrganizationalUnitIds());
                }
            } else {
                groupedStacksMap.put(compositeKey, stackInstances);
            }
        }
        return new HashSet<>(groupedStacksMap.values());
    }

    /**
     * Compares {@link StackInstance#getParameters()} with previous {@link StackInstance#getParameters()}
     * Gets the StackInstances need to update
     *
     * @param intersection     {@link StackInstance} retaining desired stack instances
     * @param previousStackMap Map contains previous stack instances
     * @return {@link StackInstance} to update
     */
    private static Set<StackInstance> getUpdatingStackInstances(
            final Set<StackInstance> intersection,
            final Map<StackInstance, StackInstance> previousStackMap) {

        return intersection.stream()
                .filter(stackInstance -> !Comparator.equals(
                        previousStackMap.get(stackInstance).getParameters(), stackInstance.getParameters()))
                .collect(Collectors.toSet());
    }

    /**
     * Since Stack instances are defined across accounts and regions with(out) parameters,
     * We are expanding all before we tack actions
     *
     * @param stackInstancesGroup {@link ResourceModel#getStackInstancesGroup()}
     * @return {@link StackInstance} set
     */
    private static Set<StackInstance> flattenStackInstancesGroup(
            final Collection<StackInstances> stackInstancesGroup, final boolean isSelfManaged) {

        final Set<StackInstance> flatStacks = new HashSet<>();
        if (CollectionUtils.isNullOrEmpty(stackInstancesGroup)) return flatStacks;

        for (final StackInstances stackInstances : stackInstancesGroup) {
            for (final String region : stackInstances.getRegions()) {

                final Set<String> targets = isSelfManaged ? stackInstances.getDeploymentTargets().getAccounts()
                        : stackInstances.getDeploymentTargets().getOrganizationalUnitIds();

                // Validates expected DeploymentTargets exist in the template
                if (CollectionUtils.isNullOrEmpty(targets)) {
                    throw new CfnInvalidRequestException(
                            String.format("%s should be specified in DeploymentTargets in [%s] model",
                                    isSelfManaged ? "Accounts" : "OrganizationalUnitIds",
                                    isSelfManaged ? "SELF_MANAGED" : "SERVICE_MANAGED"));
                }

                for (final String target : targets) {
                    final StackInstance stackInstance = StackInstance.builder()
                            .region(region).deploymentTarget(target).parameters(stackInstances.getParameterOverrides())
                            .build();

                    // Validates no duplicated stack instance is specified
                    if (flatStacks.contains(stackInstance)) {
                        throw new CfnInvalidRequestException(
                                String.format("Stack instance [%s,%s] is duplicated", target, region));
                    }

                    flatStacks.add(stackInstance);
                }
            }
        }
        return flatStacks;
    }

    /**
     * Analyzes {@link StackInstances} that need to be modified during the update operations
     *
     * @param placeHolder {@link StackInstancesPlaceHolder}
     */
    public void analyzeForUpdate(final StackInstancesPlaceHolder placeHolder) {
        final boolean isSelfManaged = isSelfManaged(desiredModel);

        final Set<StackInstance> previousStackInstances =
                flattenStackInstancesGroup(previousModel.getStackInstancesGroup(), isSelfManaged);
        final Set<StackInstance> desiredStackInstances =
                flattenStackInstancesGroup(desiredModel.getStackInstancesGroup(), isSelfManaged);

        // Calculates all necessary differences that we need to take actions
        final Set<StackInstance> stacksToAdd = new HashSet<>(desiredStackInstances);
        stacksToAdd.removeAll(previousStackInstances);
        final Set<StackInstance> stacksToDelete = new HashSet<>(previousStackInstances);
        stacksToDelete.removeAll(desiredStackInstances);
        final Set<StackInstance> stacksToCompare = new HashSet<>(desiredStackInstances);
        stacksToCompare.retainAll(previousStackInstances);

        final Set<StackInstances> stackInstancesGroupToAdd = aggregateStackInstances(stacksToAdd, isSelfManaged);
        final Set<StackInstances> stackInstancesGroupToDelete = aggregateStackInstances(stacksToDelete, isSelfManaged);

        // Since StackInstance.parameters is excluded for @EqualsAndHashCode,
        // we needs to construct a key value map to keep track on previous StackInstance objects
        final Set<StackInstance> stacksToUpdate = getUpdatingStackInstances(
                stacksToCompare, previousStackInstances.stream().collect(Collectors.toMap(s -> s, s -> s)));
        final Set<StackInstances> stackInstancesGroupToUpdate = aggregateStackInstances(stacksToUpdate, isSelfManaged);

        // Update the stack lists that need to write of callbackContext holder
        placeHolder.setCreateStackInstances(new ArrayList<>(stackInstancesGroupToAdd));
        placeHolder.setDeleteStackInstances(new ArrayList<>(stackInstancesGroupToDelete));
        placeHolder.setUpdateStackInstances(new ArrayList<>(stackInstancesGroupToUpdate));
    }

    /**
     * Analyzes {@link StackInstances} that need to be modified during create operations
     *
     * @param placeHolder {@link StackInstancesPlaceHolder}
     */
    public void analyzeForCreate(final StackInstancesPlaceHolder placeHolder) {
        if (desiredModel.getStackInstancesGroup() == null) return;
        if (desiredModel.getStackInstancesGroup().size() == 1) {
            placeHolder.setCreateStackInstances(new ArrayList<>(desiredModel.getStackInstancesGroup()));
            return;
        }
        final boolean isSelfManaged = isSelfManaged(desiredModel);

        final Set<StackInstance> desiredStackInstances =
                flattenStackInstancesGroup(desiredModel.getStackInstancesGroup(), isSelfManaged);

        final Set<StackInstances> stackInstancesGroupToAdd = aggregateStackInstances(desiredStackInstances, isSelfManaged);
        placeHolder.setCreateStackInstances(new ArrayList<>(stackInstancesGroupToAdd));
    }

    /**
     * Analyzes {@link StackInstances} that need to be modified during delete operations
     *
     * @param placeHolder {@link StackInstancesPlaceHolder}
     */
    public void analyzeForDelete(final StackInstancesPlaceHolder placeHolder) {
        if (desiredModel.getStackInstancesGroup() == null) return;
        if (desiredModel.getStackInstancesGroup().size() == 1) {
            placeHolder.setDeleteStackInstances(new ArrayList<>(desiredModel.getStackInstancesGroup()));
            return;
        }
        final boolean isSelfManaged = isSelfManaged(desiredModel);

        final Set<StackInstance> desiredStackInstances =
                flattenStackInstancesGroup(desiredModel.getStackInstancesGroup(), isSelfManaged);

        final Set<StackInstances> stackInstancesGroupToDelete = aggregateStackInstances(desiredStackInstances, isSelfManaged);
        placeHolder.setDeleteStackInstances(new ArrayList<>(stackInstancesGroupToDelete));
    }
}
