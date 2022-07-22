package software.amazon.cloudformation.stackset.translator;

import software.amazon.awssdk.services.cloudformation.model.AutoDeployment;
import software.amazon.awssdk.services.cloudformation.model.DeploymentTargets;
import software.amazon.awssdk.services.cloudformation.model.ManagedExecution;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.StackInstanceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperationPreferences;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.utils.CollectionUtils;
import software.amazon.cloudformation.stackset.OperationPreferences;
import software.amazon.cloudformation.stackset.util.StackInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertyTranslator {

    /**
     * Converts AutoDeployment (from StackSet SDK) to AutoDeployment (from CFN resource model)
     *
     * @param autoDeployment SDK AutoDeployment
     * @return Resource model AutoDeployment
     */
    public static software.amazon.cloudformation.stackset.AutoDeployment translateFromSdkAutoDeployment(
            final AutoDeployment autoDeployment) {
        if (autoDeployment == null) return null;
        return software.amazon.cloudformation.stackset.AutoDeployment.builder()
                .enabled(autoDeployment.enabled())
                .retainStacksOnAccountRemoval(autoDeployment.retainStacksOnAccountRemoval())
                .build();
    }

    /**
     * Converts AutoDeployment (from CFN resource model) to AutoDeployment (from StackSet SDK)
     *
     * @param autoDeployment AutoDeployment from resource model
     * @return SDK AutoDeployment
     */
    public static AutoDeployment translateToSdkAutoDeployment(
            final software.amazon.cloudformation.stackset.AutoDeployment autoDeployment) {
        if (autoDeployment == null) return null;
        return AutoDeployment.builder()
                .enabled(autoDeployment.getEnabled())
                .retainStacksOnAccountRemoval(autoDeployment.getRetainStacksOnAccountRemoval())
                .build();
    }

    /**
     * Converts resource model DeploymentTargets to StackSet SDK DeploymentTargets
     *
     * @param deploymentTargets DeploymentTargets from resource model
     * @return SDK DeploymentTargets
     */
    static DeploymentTargets translateToSdkDeploymentTargets(
            final software.amazon.cloudformation.stackset.DeploymentTargets deploymentTargets) {
        return DeploymentTargets.builder()
                .accounts(deploymentTargets.getAccounts())
                .organizationalUnitIds(deploymentTargets.getOrganizationalUnitIds())
                .accountFilterType(deploymentTargets.getAccountFilterType())
                .build();
    }

    /**
     * Converts StackSet SDK Parameters to resource model Parameters
     *
     * @param parameters Parameters collection from resource model
     * @return SDK Parameter list
     */
    static List<Parameter> translateToSdkParameters(
            final Collection<software.amazon.cloudformation.stackset.Parameter> parameters) {
        // To remove Parameters from a StackSet or StackSetInstance, set it as an empty list
        if (CollectionUtils.isNullOrEmpty(parameters)) return Collections.emptyList();
        return parameters.stream()
                .map(parameter -> Parameter.builder()
                        .parameterKey(parameter.getParameterKey())
                        .parameterValue(parameter.getParameterValue())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Converts resource model Parameters to StackSet SDK Parameters
     *
     * @param parameters Parameters from SDK
     * @return resource model Parameters
     */
    public static Set<software.amazon.cloudformation.stackset.Parameter> translateFromSdkParameters(
            final Collection<Parameter> parameters) {
        if (CollectionUtils.isNullOrEmpty(parameters)) return null;
        return parameters.stream()
                .map(parameter -> software.amazon.cloudformation.stackset.Parameter.builder()
                        .parameterKey(parameter.parameterKey())
                        .parameterValue(parameter.parameterValue())
                        .build())
                .collect(Collectors.toSet());
    }

    /**
     * Converts resource model OperationPreferences to StackSet SDK OperationPreferences
     *
     * @param operationPreferences OperationPreferences from resource model
     * @return SDK OperationPreferences
     */
    static StackSetOperationPreferences translateToSdkOperationPreferences(
            final OperationPreferences operationPreferences) {
        if (operationPreferences == null) return null;
        return StackSetOperationPreferences.builder()
                .maxConcurrentCount(operationPreferences.getMaxConcurrentCount())
                .maxConcurrentPercentage(operationPreferences.getMaxConcurrentPercentage())
                .failureToleranceCount(operationPreferences.getFailureToleranceCount())
                .failureTolerancePercentage(operationPreferences.getFailureTolerancePercentage())
                .regionOrder(operationPreferences.getRegionOrder())
                .regionConcurrencyType(operationPreferences.getRegionConcurrencyType())
                .build();
    }

    /**
     * Converts resource model ManagedExecution to StackSet SDK ManagedExecution
     *
     * @param managedExecution ManagedExecution from resource model
     * @return SDK ManagedExecution
     */
    static ManagedExecution translateToSdkManagedExecution(
            final software.amazon.cloudformation.stackset.ManagedExecution managedExecution) {
        return ManagedExecution.builder()
                .active((managedExecution == null || managedExecution.getActive() == null)
                        ? false : managedExecution.getActive())
                .build();
    }

    /**
     * Converts StackSet SDK ManagedExecution to resource model ManagedExecution
     *
     * @param managedExecution ManagedExecution from SDK
     * @return resource model ManagedExecution
     */
    public static software.amazon.cloudformation.stackset.ManagedExecution translateFromSdkManagedExecution(
            final ManagedExecution managedExecution) {
        if (managedExecution == null) return null;
        return software.amazon.cloudformation.stackset.ManagedExecution.builder()
                .active(managedExecution.active())
                .build();
    }

    /**
     * Converts tags (from CFN resource model) to StackSet set (from StackSet SDK)
     *
     * @param tags Tags CFN resource model.
     * @return SDK Tags.
     */
    public static Set<Tag> translateToSdkTags(final Map<String, String> tags) {
        if (tags == null) {
            return Collections.emptySet();
        }
        return Optional.of(tags.entrySet()).orElse(Collections.emptySet())
                .stream()
                .map(tag -> Tag.builder().key(tag.getKey()).value(tag.getValue()).build())
                .collect(Collectors.toSet());
    }

    /**
     * Converts a list of tags (from StackSet SDK) to HostedZoneTag set (from CFN resource model)
     *
     * @param tags Tags from StackSet SDK.
     * @return A set of CFN StackSet Tag.
     */
    public static Set<software.amazon.cloudformation.stackset.Tag> translateFromSdkTags(final Collection<Tag> tags) {
        if (CollectionUtils.isNullOrEmpty(tags)) return null;
        return tags.stream().map(tag -> software.amazon.cloudformation.stackset.Tag.builder()
                .key(tag.key())
                .value(tag.value())
                .build())
                .collect(Collectors.toSet());
    }

    /**
     * Converts {@link StackInstanceSummary} to {@link StackInstance} utility placeholder
     *
     * @param isSelfManaged if PermissionModel is SELF_MANAGED
     * @param summary       {@link StackInstanceSummary}
     * @return {@link StackInstance}
     */
    public static StackInstance translateToStackInstance(
            final boolean isSelfManaged,
            final StackInstanceSummary summary,
            final Collection<Parameter> parameters) {

        return StackInstance.builder()
                .region(summary.region())
                .parameters(translateFromSdkParameters(parameters))
                .deploymentTarget(isSelfManaged ? summary.account() : summary.organizationalUnitId())
                .build();
    }
}
