package software.amazon.cloudformation.stackset.translator;

import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackInstanceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetOperationResultsRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetsRequest;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetRequest;
import software.amazon.cloudformation.stackset.OperationPreferences;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static software.amazon.awssdk.services.cloudformation.model.StackSetStatus.ACTIVE;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkAutoDeployment;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkDeploymentTargets;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkManagedExecution;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkOperationPreferences;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkTags;

public class RequestTranslator {

    private static final int LIST_MAX_ITEMS = 100;

    public static CreateStackSetRequest createStackSetRequest(
            final ResourceModel model, final String requestToken, final Map<String, String> tags) {
        return CreateStackSetRequest.builder()
                .stackSetName(model.getStackSetName())
                .administrationRoleARN(model.getAdministrationRoleARN())
                .autoDeployment(translateToSdkAutoDeployment(model.getAutoDeployment()))
                .clientRequestToken(requestToken)
                .permissionModel(model.getPermissionModel())
                .capabilitiesWithStrings(model.getCapabilities())
                .description(model.getDescription())
                .executionRoleName(model.getExecutionRoleName())
                .parameters(translateToSdkParameters(model.getParameters()))
                .tags(translateToSdkTags(tags))
                .templateBody(model.getTemplateBody())
                .templateURL(model.getTemplateURL())
                .callAs(model.getCallAs())
                .managedExecution(translateToSdkManagedExecution(model.getManagedExecution()))
                .build();
    }

    public static CreateStackInstancesRequest createStackInstancesRequest(
            final String stackSetName,
            final OperationPreferences operationPreferences,
            final StackInstances stackInstances,
            final String callAs) {
        return CreateStackInstancesRequest.builder()
                .stackSetName(stackSetName)
                .regions(stackInstances.getRegions())
                .operationPreferences(translateToSdkOperationPreferences(operationPreferences))
                .deploymentTargets(translateToSdkDeploymentTargets(stackInstances.getDeploymentTargets()))
                .parameterOverrides(translateToSdkParameters(stackInstances.getParameterOverrides()))
                .callAs(callAs)
                .build();
    }

    public static UpdateStackInstancesRequest updateStackInstancesRequest(
            final String stackSetName,
            final OperationPreferences operationPreferences,
            final StackInstances stackInstances,
            final String callAs) {
        return UpdateStackInstancesRequest.builder()
                .stackSetName(stackSetName)
                .regions(stackInstances.getRegions())
                .operationPreferences(translateToSdkOperationPreferences(operationPreferences))
                .deploymentTargets(translateToSdkDeploymentTargets(stackInstances.getDeploymentTargets()))
                .parameterOverrides(translateToSdkParameters(stackInstances.getParameterOverrides()))
                .callAs(callAs)
                .build();
    }

    public static DeleteStackSetRequest deleteStackSetRequest(
            final String stackSetName,
            final String callAs) {
        return DeleteStackSetRequest.builder()
                .stackSetName(stackSetName)
                .callAs(callAs)
                .build();
    }

    public static DeleteStackInstancesRequest deleteStackInstancesRequest(
            final String stackSetName,
            final OperationPreferences operationPreferences,
            final StackInstances stackInstances,
            final String callAs) {
        return DeleteStackInstancesRequest.builder()
                .stackSetName(stackSetName)
                .regions(stackInstances.getRegions())
                .operationPreferences(translateToSdkOperationPreferences(operationPreferences))
                .deploymentTargets(translateToSdkDeploymentTargets(stackInstances.getDeploymentTargets()))
                .callAs(callAs)
                .build();
    }

    public static UpdateStackSetRequest updateStackSetRequest(
            final ResourceModel model,
            final Map<String, String> previousTags,
            final Map<String, String> tags,
            final List<Tag> currentTags) {
        Set<Tag> activeStackSetTags = new HashSet<>(currentTags);
        Set<Tag> templateStackSetTags = translateToSdkTags(tags);
        Set<Tag> previousTemplateTagSet = translateToSdkTags(previousTags);
        // Construct out-of-band tag set by performing a set difference of active tags and tags from previous model
        Set<Tag> outOfBandTags = new HashSet<>(activeStackSetTags);
        outOfBandTags.removeAll(previousTemplateTagSet);

        // if the out of band tag doesn't already exist in tagsToSet add it
        Set<String> templateTagKeys = templateStackSetTags.stream().map(Tag::key).collect(Collectors.toSet());
        Set<String> outOfBandTagKeys = outOfBandTags.stream().map(Tag::key).collect(Collectors.toSet());
        Map<String, Tag> outOfBandTagMap = getTagMapFromSet(outOfBandTags);
        Set<Tag> tagsToSet = new HashSet<>(templateStackSetTags);
        for (String tagKey: outOfBandTagKeys) {
            if (!templateTagKeys.contains(tagKey)) {
                tagsToSet.add(outOfBandTagMap.get(tagKey));
            }
        }

        return UpdateStackSetRequest.builder()
                .stackSetName(model.getStackSetId())
                .administrationRoleARN(model.getAdministrationRoleARN())
                .autoDeployment(translateToSdkAutoDeployment(model.getAutoDeployment()))
                .operationPreferences(translateToSdkOperationPreferences(model.getOperationPreferences()))
                .capabilitiesWithStrings(model.getCapabilities())
                .description(model.getDescription())
                .executionRoleName(model.getExecutionRoleName())
                .parameters(translateToSdkParameters(model.getParameters()))
                .templateURL(model.getTemplateURL())
                .templateBody(model.getTemplateBody())
                .tags(tagsToSet)
                .callAs(model.getCallAs())
                .build();
    }

    private static Map<String, Tag> getTagMapFromSet(Set<Tag> tagSet) {
        Map<String, Tag> tagSetToMap = new HashMap<>();
        tagSet.forEach(tag -> {
            tagSetToMap.put(tag.key(), tag);
        });
        return tagSetToMap;
    }

    public static UpdateStackSetRequest updateManagedExecutionRequest(
            final ResourceModel model) {
        return UpdateStackSetRequest.builder()
                .stackSetName(model.getStackSetId())
                .managedExecution(translateToSdkManagedExecution(model.getManagedExecution()))
                .administrationRoleARN(model.getAdministrationRoleARN()) // In case Customized Role was used during CREATE
                .executionRoleName(model.getExecutionRoleName()) // In case Customized Role was used during CREATE
                .capabilitiesWithStrings(model.getCapabilities()) // Ideally, we do not need this, in case UpdateManagedExecution request accidentally triggers deployments
                .usePreviousTemplate(true)
                .callAs(model.getCallAs())
                .build();
    }

    public static ListStackSetsRequest listStackSetsRequest(
            final String nextToken) {
        return ListStackSetsRequest.builder()
                .maxResults(LIST_MAX_ITEMS)
                .nextToken(nextToken)
                .status(ACTIVE)
                .build();
    }

    public static ListStackInstancesRequest listStackInstancesRequest(
            final String nextToken,
            final String stackSetName,
            final String callAs) {
        return ListStackInstancesRequest.builder()
                .maxResults(LIST_MAX_ITEMS)
                .nextToken(nextToken)
                .stackSetName(stackSetName)
                .callAs(callAs)
                .build();
    }

    public static DescribeStackSetRequest describeStackSetRequest(
            final String stackSetId,
            final String callAs) {
        return DescribeStackSetRequest.builder()
                .stackSetName(stackSetId)
                .callAs(callAs)
                .build();
    }

    public static DescribeStackInstanceRequest describeStackInstanceRequest(
            final String account,
            final String region,
            final String stackSetId,
            final String callAs) {
        return DescribeStackInstanceRequest.builder()
                .stackInstanceAccount(account)
                .stackInstanceRegion(region)
                .stackSetName(stackSetId)
                .callAs(callAs)
                .build();
    }

    public static DescribeStackSetOperationRequest describeStackSetOperationRequest(
            final String stackSetName,
            final String operationId,
            final String callAs) {
        return DescribeStackSetOperationRequest.builder()
                .stackSetName(stackSetName)
                .operationId(operationId)
                .callAs(callAs)
                .build();
    }

    public static GetTemplateSummaryRequest getTemplateSummaryRequest(
            final String templateBody,
            final String templateUrl) {
        return GetTemplateSummaryRequest.builder()
                .templateBody(templateBody)
                .templateURL(templateUrl)
                .build();
    }

    public static ListStackSetOperationResultsRequest listStackSetOperationResultsRequest(
            final String nextToken,
            final String stackSetName,
            final String operationId,
            final String callAs) {
        return ListStackSetOperationResultsRequest.builder()
                .maxResults(LIST_MAX_ITEMS)
                .nextToken(nextToken)
                .stackSetName(stackSetName)
                .operationId(operationId)
                .callAs(callAs)
                .build();
    }
}
