package software.amazon.cloudformation.stackset.translator;

import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackInstanceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetsRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.cloudformation.stackset.OperationPreferences;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkAutoDeployment;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkDeploymentTargets;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkOperationPreferences;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkTags;

public class RequestTranslator {

    private static int LIST_MAX_ITEMS = 100;

    public static CreateStackSetRequest createStackSetRequest(
            final ResourceModel model, final String stackSetName, final String requestToken) {
        return CreateStackSetRequest.builder()
                .stackSetName(stackSetName)
                .administrationRoleARN(model.getAdministrationRoleARN())
                .autoDeployment(translateToSdkAutoDeployment(model.getAutoDeployment()))
                .clientRequestToken(requestToken)
                .permissionModel(model.getPermissionModel())
                .capabilitiesWithStrings(model.getCapabilities())
                .description(model.getDescription())
                .executionRoleName(model.getExecutionRoleName())
                .parameters(translateToSdkParameters(model.getParameters()))
                .tags(translateToSdkTags(model.getTags()))
                .templateBody(model.getTemplateBody())
                .templateURL(model.getTemplateURL())
                .build();
    }

    public static CreateStackInstancesRequest createStackInstancesRequest(
            final String stackSetName,
            final OperationPreferences operationPreferences,
            final StackInstances stackInstances) {
        return CreateStackInstancesRequest.builder()
                .stackSetName(stackSetName)
                .regions(stackInstances.getRegions())
                .operationPreferences(translateToSdkOperationPreferences(operationPreferences))
                .deploymentTargets(translateToSdkDeploymentTargets(stackInstances.getDeploymentTargets()))
                .parameterOverrides(translateToSdkParameters(stackInstances.getParameterOverrides()))
                .build();
    }

    public static UpdateStackInstancesRequest updateStackInstancesRequest(
            final String stackSetName,
            final OperationPreferences operationPreferences,
            final StackInstances stackInstances) {
        return UpdateStackInstancesRequest.builder()
                .stackSetName(stackSetName)
                .regions(stackInstances.getRegions())
                .operationPreferences(translateToSdkOperationPreferences(operationPreferences))
                .deploymentTargets(translateToSdkDeploymentTargets(stackInstances.getDeploymentTargets()))
                .parameterOverrides(translateToSdkParameters(stackInstances.getParameterOverrides()))
                .build();
    }

    public static DeleteStackSetRequest deleteStackSetRequest(final String stackSetName) {
        return DeleteStackSetRequest.builder()
                .stackSetName(stackSetName)
                .build();
    }

    public static DeleteStackInstancesRequest deleteStackInstancesRequest(
            final String stackSetName,
            final OperationPreferences operationPreferences,
            final StackInstances stackInstances) {
        return DeleteStackInstancesRequest.builder()
                .stackSetName(stackSetName)
                .regions(stackInstances.getRegions())
                .operationPreferences(translateToSdkOperationPreferences(operationPreferences))
                .deploymentTargets(translateToSdkDeploymentTargets(stackInstances.getDeploymentTargets()))
                .build();
    }

    public static UpdateStackSetRequest updateStackSetRequest(final ResourceModel model) {
        return UpdateStackSetRequest.builder()
                .stackSetName(model.getStackSetId())
                .administrationRoleARN(model.getAdministrationRoleARN())
                .autoDeployment(translateToSdkAutoDeployment(model.getAutoDeployment()))
                .capabilitiesWithStrings(model.getCapabilities())
                .description(model.getDescription())
                .executionRoleName(model.getExecutionRoleName())
                .parameters(translateToSdkParameters(model.getParameters()))
                .templateURL(model.getTemplateURL())
                .templateBody(model.getTemplateBody())
                .tags(translateToSdkTags(model.getTags()))
                .build();
    }

    public static ListStackSetsRequest listStackSetsRequest(final String nextToken) {
        return ListStackSetsRequest.builder()
                .maxResults(LIST_MAX_ITEMS)
                .nextToken(nextToken)
                .build();
    }

    public static ListStackInstancesRequest listStackInstancesRequest(
            final String nextToken, final String stackSetName) {
        return ListStackInstancesRequest.builder()
                .maxResults(LIST_MAX_ITEMS)
                .nextToken(nextToken)
                .stackSetName(stackSetName)
                .build();
    }

    public static DescribeStackSetRequest describeStackSetRequest(final String stackSetId) {
        return DescribeStackSetRequest.builder()
                .stackSetName(stackSetId)
                .build();
    }

    public static DescribeStackInstanceRequest describeStackInstanceRequest(
            final String account,
            final String region,
            final String stackSetId) {
        return DescribeStackInstanceRequest.builder()
                .stackInstanceAccount(account)
                .stackInstanceRegion(region)
                .stackSetName(stackSetId)
                .build();
    }

    public static DescribeStackSetOperationRequest describeStackSetOperationRequest(
            final String stackSetName, final String operationId) {
        return DescribeStackSetOperationRequest.builder()
                .stackSetName(stackSetName)
                .operationId(operationId)
                .build();
    }

    public static GetObjectRequest getObjectRequest(
            final String bucketName, final String key) {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
    }
}
