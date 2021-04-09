package software.amazon.cloudformation.stackset.util;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackInstanceResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.GetTemplateSummaryResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetsResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
import software.amazon.awssdk.services.cloudformation.model.RegionConcurrencyType;
import software.amazon.awssdk.services.cloudformation.model.StackInstance;
import software.amazon.awssdk.services.cloudformation.model.StackInstanceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackSet;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperation;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperationStatus;
import software.amazon.awssdk.services.cloudformation.model.StackSetSummary;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackSetResponse;
import software.amazon.cloudformation.stackset.AutoDeployment;
import software.amazon.cloudformation.stackset.DeploymentTargets;
import software.amazon.cloudformation.stackset.OperationPreferences;
import software.amazon.cloudformation.stackset.ResourceModel;
import software.amazon.cloudformation.stackset.StackInstances;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class TestUtils {

    public final static String DESCRIPTION = "description";
    public final static String UPDATED_DESCRIPTION = "description-new";

    public final static String ADMINISTRATION_ROLE_ARN = "administration:role:arn";
    public final static String UPDATED_ADMINISTRATION_ROLE_ARN = "administration:role:arn-new";

    public final static String EXECUTION_ROLE_NAME = "execution:role:arn";
    public final static String UPDATED_EXECUTION_ROLE_NAME = "execution:role:arn-new";

    public final static String TEMPLATE_URL = "http://s3-us-west-2.amazonaws.com/example/example.json";
    public final static String UPDATED_TEMPLATE_URL = "http://s3-us-west-2.amazonaws.com/example/new-example.json";

    public final static String TEMPLATE_BODY = new StringBuilder()
            .append("{\n")
            .append("  \"AWSTemplateFormatVersion\" : \"2010-09-09\",\n")
            .append("  \"Resources\" : {\n")
            .append("    \"IntegrationTestWaitHandle\" : {\n")
            .append("      \"Type\" : \"AWS::CloudFormation::WaitConditionHandle\",\n")
            .append("      \"Properties\" : {\n")
            .append("      }\n")
            .append("    }\n")
            .append("  }\n")
            .append("}").toString();

    public final static String UPDATED_TEMPLATE_BODY = new StringBuilder()
            .append("{\n")
            .append("  \"AWSTemplateFormatVersion\" : \"2010-09-09\",\n")
            .append("  \"Resources\" : {\n")
            .append("    \"IntegrationTestWaitHandle\" : {\n")
            .append("      \"Type\" : \"AWS::CloudFormation::WaitCondition\",\n")
            .append("      \"Properties\" : {\n")
            .append("      }\n")
            .append("    }\n")
            .append("  }\n")
            .append("}").toString();

    public final static String STACK_SET_NAME = "StackSet";
    public final static String STACK_SET_ID = "StackSet:stack-set-id";

    public final static String OPERATION_ID_1 = "operation-id-1";

    public final static String LOGICAL_ID = "MyResource";
    public final static String REQUEST_TOKEN = "token";

    public final static String SERVICE_MANAGED = "SERVICE_MANAGED";
    public final static String SELF_MANAGED = "SELF_MANAGED";

    public final static String US_EAST_1 = "us-east-1";
    public final static String US_WEST_1 = "us-west-1";
    public final static String US_EAST_2 = "us-east-2";
    public final static String US_WEST_2 = "us-west-2";

    public final static String EU_EAST_1 = "eu-east-1";
    public final static String EU_EAST_2 = "eu-east-2";
    public final static String EU_EAST_3 = "eu-east-3";
    public final static String EU_CENTRAL_1 = "eu-central-1";
    public final static String EU_NORTH_1 = "eu-north-1";

    public final static String ORGANIZATION_UNIT_ID_1 = "ou-example-1";
    public final static String ORGANIZATION_UNIT_ID_2 = "ou-example-2";
    public final static String ORGANIZATION_UNIT_ID_3 = "ou-example-3";
    public final static String ORGANIZATION_UNIT_ID_4 = "ou-example-4";

    public final static String ACCOUNT_ID_1 = "111111111111";
    public final static String ACCOUNT_ID_2 = "222222222222";
    public final static String ACCOUNT_ID_3 = "333333333333";
    public final static String ACCOUNT_ID_4 = "444444444444";
    public final static String ACCOUNT_ID_5 = "555555555555";
    public final static String ACCOUNT_ID_6 = "666666666666";

    public final static String PARAMETER_KEY_1 = "parameter_key_1";
    public final static String PARAMETER_KEY_2 = "parameter_key_3";
    public final static String PARAMETER_KEY_3 = "parameter_key_3";

    public final static String PARAMETER_VALUE_1 = "parameter_value_1";
    public final static String PARAMETER_VALUE_2 = "parameter_value_2";
    public final static String PARAMETER_VALUE_3 = "parameter_value_3";

    public final static software.amazon.cloudformation.stackset.Parameter PARAMETER_1 =
            software.amazon.cloudformation.stackset.Parameter.builder()
                    .parameterKey(PARAMETER_KEY_1)
                    .parameterValue(PARAMETER_VALUE_1)
                    .build();

    public final static software.amazon.cloudformation.stackset.Parameter PARAMETER_1_COPY =
            software.amazon.cloudformation.stackset.Parameter.builder()
                    .parameterKey(PARAMETER_KEY_1)
                    .parameterValue(PARAMETER_VALUE_1)
                    .build();

    public final static software.amazon.cloudformation.stackset.Parameter PARAMETER_1_UPDATED =
            software.amazon.cloudformation.stackset.Parameter.builder()
                    .parameterKey(PARAMETER_KEY_1)
                    .parameterValue(PARAMETER_VALUE_2)
                    .build();

    public final static software.amazon.cloudformation.stackset.Parameter PARAMETER_2 =
            software.amazon.cloudformation.stackset.Parameter.builder()
                    .parameterKey(PARAMETER_KEY_2)
                    .parameterValue(PARAMETER_VALUE_2)
                    .build();

    public final static software.amazon.cloudformation.stackset.Parameter PARAMETER_3 =
            software.amazon.cloudformation.stackset.Parameter.builder()
                    .parameterKey(PARAMETER_KEY_3)
                    .parameterValue(PARAMETER_VALUE_3)
                    .build();

    public final static Parameter SDK_PARAMETER_1 = Parameter.builder()
            .parameterKey(PARAMETER_KEY_1)
            .parameterValue(PARAMETER_VALUE_1)
            .build();

    public final static Parameter SDK_PARAMETER_2 = Parameter.builder()
            .parameterKey(PARAMETER_KEY_2)
            .parameterValue(PARAMETER_VALUE_2)
            .build();

    public final static Parameter SDK_PARAMETER_3 = Parameter.builder()
            .parameterKey(PARAMETER_KEY_3)
            .parameterValue(PARAMETER_VALUE_3)
            .build();

    public final static Map<String, String> DESIRED_RESOURCE_TAGS = ImmutableMap.of(
            "key1", "val1", "key2", "val2", "key3", "val3");
    public final static Map<String, String> PREVIOUS_RESOURCE_TAGS = ImmutableMap.of(
            "key-1", "val1", "key-2", "val2", "key-3", "val3");
    public final static Map<String, String> NEW_RESOURCE_TAGS = ImmutableMap.of(
            "key1", "val1", "key2updated", "val2updated", "key3", "val3");

    public final static Set<String> REGIONS_1 = new HashSet<>(Arrays.asList(US_WEST_1, US_EAST_1));
    public final static Set<String> UPDATED_REGIONS_1 = new HashSet<>(Arrays.asList(US_WEST_1, US_EAST_2));

    public final static Set<String> REGIONS_2 = new HashSet<>(Arrays.asList(EU_EAST_1, EU_EAST_2));
    public final static Set<String> UPDATED_REGIONS_2 = new HashSet<>(Arrays.asList(EU_EAST_3, EU_CENTRAL_1));

    public final static DeploymentTargets SERVICE_MANAGED_TARGETS = DeploymentTargets.builder()
            .organizationalUnitIds(new HashSet<>(Arrays.asList(
                    ORGANIZATION_UNIT_ID_1, ORGANIZATION_UNIT_ID_2)))
            .build();

    public final static DeploymentTargets UPDATED_SERVICE_MANAGED_TARGETS = DeploymentTargets.builder()
            .organizationalUnitIds(new HashSet<>(Arrays.asList(
                    ORGANIZATION_UNIT_ID_3, ORGANIZATION_UNIT_ID_4)))
            .build();

    public final static DeploymentTargets SELF_MANAGED_TARGETS = DeploymentTargets.builder()
            .accounts(new HashSet<>(Arrays.asList(
                    ACCOUNT_ID_1)))
            .build();

    public final static DeploymentTargets UPDATED_SELF_MANAGED_TARGETS = DeploymentTargets.builder()
            .accounts(new HashSet<>(Arrays.asList(
                    ACCOUNT_ID_2)))
            .build();

    public final static Set<String> CAPABILITIES = new HashSet<>(Arrays.asList(
            "CAPABILITY_IAM", "CAPABILITY_NAMED_IAM"));

    public final static OperationPreferences OPERATION_PREFERENCES = OperationPreferences.builder()
            .failureToleranceCount(0)
            .maxConcurrentCount(1)
            .build();

    public final static OperationPreferences OPERATION_PREFERENCES_FULL = OperationPreferences.builder()
            .failureToleranceCount(0)
            .failureTolerancePercentage(0)
            .maxConcurrentCount(1)
            .maxConcurrentPercentage(100)
            .regionOrder(Arrays.asList(US_WEST_1, US_EAST_1))
            .regionConcurrencyType(RegionConcurrencyType.PARALLEL.toString())
            .build();

    public final static Set<software.amazon.cloudformation.stackset.Tag> TAGS = new HashSet<>(Arrays.asList(
            new software.amazon.cloudformation.stackset.Tag("key1", "val1"),
            new software.amazon.cloudformation.stackset.Tag("key2", "val2"),
            new software.amazon.cloudformation.stackset.Tag("key3", "val3")));

    public final static Set<software.amazon.cloudformation.stackset.Tag> TAGS_TO_UPDATE = new HashSet<>(Arrays.asList(
            new software.amazon.cloudformation.stackset.Tag("key-1", "val1"),
            new software.amazon.cloudformation.stackset.Tag("key-2", "val2"),
            new software.amazon.cloudformation.stackset.Tag("key-3", "val3")));

    public final static Set<Tag> TAGGED_RESOURCES = new HashSet<>(Arrays.asList(
            Tag.builder().key("key1").value("val1").build(),
            Tag.builder().key("key2").value("val2").build(),
            Tag.builder().key("key3").value("val3").build()));

    public final static Set<Tag> SDK_TAGS_TO_UPDATE = new HashSet<>(Arrays.asList(
            Tag.builder().key("key-1").value("val1").build(),
            Tag.builder().key("key-2").value("val2").build(),
            Tag.builder().key("key-3").value("val3").build()));

    public final static AutoDeployment AUTO_DEPLOYMENT_ENABLED = AutoDeployment.builder()
            .enabled(true)
            .retainStacksOnAccountRemoval(true)
            .build();

    public final static AutoDeployment AUTO_DEPLOYMENT_ENABLED_COPY = AutoDeployment.builder()
            .enabled(true)
            .retainStacksOnAccountRemoval(true)
            .build();

    public final static AutoDeployment AUTO_DEPLOYMENT_DISABLED = AutoDeployment.builder()
            .enabled(false)
            .retainStacksOnAccountRemoval(false)
            .build();

    public final static GetTemplateSummaryResponse VALID_TEMPLATE_SUMMARY_RESPONSE = GetTemplateSummaryResponse.builder()
            .resourceTypes(Arrays.asList("AWS::CloudFormation::WaitCondition"))
            .build();

    public final static GetTemplateSummaryResponse TEMPLATE_SUMMARY_RESPONSE_WITH_NESTED_STACK =
            GetTemplateSummaryResponse.builder()
                    .resourceTypes(Arrays.asList("AWS::CloudFormation::Stack"))
                    .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_1 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_1)
            .account(ACCOUNT_ID_1)
            .region(US_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_2 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_1)
            .account(ACCOUNT_ID_1)
            .region(US_WEST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_3 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_2)
            .account(ACCOUNT_ID_2)
            .region(EU_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_4 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_2)
            .account(ACCOUNT_ID_2)
            .region(EU_EAST_2)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_5 = StackInstanceSummary.builder()
            .account(ACCOUNT_ID_1)
            .region(US_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_6 = StackInstanceSummary.builder()
            .account(ACCOUNT_ID_1)
            .region(US_WEST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_7 = StackInstanceSummary.builder()
            .account(ACCOUNT_ID_2)
            .region(EU_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_8 = StackInstanceSummary.builder()
            .account(ACCOUNT_ID_2)
            .region(EU_EAST_2)
            .build();

    public final static StackInstance STACK_INSTANCE_1 = StackInstance.builder()
            .account(ACCOUNT_ID_1)
            .region(US_EAST_1)
            .build();

    public final static DescribeStackInstanceResponse DESCRIBE_STACK_INSTANCE_RESPONSE_1 =
            DescribeStackInstanceResponse.builder()
                    .stackInstance(STACK_INSTANCE_1)
                    .build();

    public final static StackInstance STACK_INSTANCE_2 = StackInstance.builder()
            .account(ACCOUNT_ID_1)
            .region(US_WEST_1)
            .build();

    public final static DescribeStackInstanceResponse DESCRIBE_STACK_INSTANCE_RESPONSE_2 =
            DescribeStackInstanceResponse.builder()
                    .stackInstance(STACK_INSTANCE_2)
                    .build();

    public final static StackInstance STACK_INSTANCE_3 = StackInstance.builder()
            .account(ACCOUNT_ID_2)
            .region(EU_EAST_1)
            .build();

    public final static DescribeStackInstanceResponse DESCRIBE_STACK_INSTANCE_RESPONSE_3 =
            DescribeStackInstanceResponse.builder()
                    .stackInstance(STACK_INSTANCE_3)
                    .build();

    public final static StackInstance STACK_INSTANCE_4 = StackInstance.builder()
            .account(ACCOUNT_ID_2)
            .region(EU_EAST_2)
            .build();

    public final static DescribeStackInstanceResponse DESCRIBE_STACK_INSTANCE_RESPONSE_4 =
            DescribeStackInstanceResponse.builder()
                    .stackInstance(STACK_INSTANCE_4)
                    .build();

    public final static List<StackInstanceSummary> SERVICE_MANAGED_STACK_INSTANCE_SUMMARIES = Arrays.asList(
            STACK_INSTANCE_SUMMARY_1, STACK_INSTANCE_SUMMARY_2, STACK_INSTANCE_SUMMARY_3, STACK_INSTANCE_SUMMARY_4);

    public final static List<StackInstanceSummary> SELF_MANAGED_STACK_INSTANCE_SUMMARIES = Arrays.asList(
            STACK_INSTANCE_SUMMARY_5, STACK_INSTANCE_SUMMARY_6, STACK_INSTANCE_SUMMARY_7, STACK_INSTANCE_SUMMARY_8);

    public final static List<StackInstanceSummary> SELF_MANAGED_STACK_ONE_INSTANCES_SUMMARIES = Arrays.asList(
            STACK_INSTANCE_SUMMARY_5, STACK_INSTANCE_SUMMARY_6);

    public final static software.amazon.awssdk.services.cloudformation.model.AutoDeployment SDK_AUTO_DEPLOYMENT =
            software.amazon.awssdk.services.cloudformation.model.AutoDeployment.builder()
                    .retainStacksOnAccountRemoval(true)
                    .enabled(true)
                    .build();

    public final static StackInstances SERVICE_MANAGED_STACK_INSTANCES_1 = StackInstances.builder()
            .regions(REGIONS_1)
            .deploymentTargets(SERVICE_MANAGED_TARGETS)
            .build();

    public final static StackInstances SERVICE_MANAGED_STACK_INSTANCES_2 = StackInstances.builder()
            .regions(REGIONS_1)
            .deploymentTargets(UPDATED_SERVICE_MANAGED_TARGETS)
            .build();

    public final static StackInstances SELF_MANAGED_STACK_INSTANCES_1 = StackInstances.builder()
            .regions(REGIONS_1)
            .deploymentTargets(SELF_MANAGED_TARGETS)
            .build();

    public final static StackInstances SELF_MANAGED_STACK_INSTANCES_2 = StackInstances.builder()
            .regions(REGIONS_2)
            .deploymentTargets(UPDATED_SELF_MANAGED_TARGETS)
            .build();

    public final static StackInstances SELF_MANAGED_STACK_INSTANCES_3 = StackInstances.builder()
            .regions(UPDATED_REGIONS_1)
            .deploymentTargets(SELF_MANAGED_TARGETS)
            .build();

    public final static StackInstances SELF_MANAGED_STACK_INSTANCES_4 = StackInstances.builder()
            .regions(REGIONS_2)
            .deploymentTargets(UPDATED_SELF_MANAGED_TARGETS)
            .parameterOverrides(new HashSet<>(Arrays.asList(PARAMETER_1)))
            .build();

    public final static StackSetSummary STACK_SET_SUMMARY_1 = StackSetSummary.builder()
            .description(DESCRIPTION)
            .permissionModel(PermissionModels.SELF_MANAGED)
            .stackSetId(STACK_SET_ID)
            .stackSetName(STACK_SET_NAME)
            .build();

    public final static StackSet SERVICE_MANAGED_STACK_SET = StackSet.builder()
            .stackSetId(STACK_SET_ID)
            .stackSetName(STACK_SET_NAME)
            .autoDeployment(SDK_AUTO_DEPLOYMENT)
            .capabilitiesWithStrings(CAPABILITIES)
            .description(DESCRIPTION)
            .organizationalUnitIds(ORGANIZATION_UNIT_ID_1, ORGANIZATION_UNIT_ID_2)
            .parameters(SDK_PARAMETER_1, SDK_PARAMETER_2)
            .permissionModel(PermissionModels.SERVICE_MANAGED)
            .templateBody(TEMPLATE_BODY)
            .tags(TAGGED_RESOURCES)
            .build();

    public final static StackSet SELF_MANAGED_STACK_SET = StackSet.builder()
            .stackSetId(STACK_SET_ID)
            .stackSetName(STACK_SET_NAME)
            .capabilitiesWithStrings(CAPABILITIES)
            .description(DESCRIPTION)
            .parameters(SDK_PARAMETER_1, SDK_PARAMETER_2)
            .templateBody(TEMPLATE_BODY)
            .permissionModel(PermissionModels.SELF_MANAGED)
            .tags(TAGGED_RESOURCES)
            .build();

    public final static StackSet NULL_PERMISSION_MODEL_STACK_SET = StackSet.builder()
            .stackSetId(STACK_SET_ID)
            .stackSetName(STACK_SET_NAME)
            .capabilitiesWithStrings(CAPABILITIES)
            .description(DESCRIPTION)
            .parameters(SDK_PARAMETER_1, SDK_PARAMETER_2)
            .templateBody(TEMPLATE_BODY)
            .tags(TAGGED_RESOURCES)
            .build();

    public final static ResourceModel SERVICE_MANAGED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SERVICE_MANAGED)
            .capabilities(CAPABILITIES)
            .description(DESCRIPTION)
            .autoDeployment(AUTO_DEPLOYMENT_ENABLED)
            .templateBody(TEMPLATE_BODY)
            .stackInstancesGroup(new HashSet<>(Arrays.asList(SERVICE_MANAGED_STACK_INSTANCES_1, SERVICE_MANAGED_STACK_INSTANCES_2)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .operationPreferences(OPERATION_PREFERENCES)
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_1, SELF_MANAGED_STACK_INSTANCES_2)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .operationPreferences(OPERATION_PREFERENCES)
            .tags(TAGS)
            .build();

    public final static ResourceModel UPDATED_SELF_MANAGED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_3, SELF_MANAGED_STACK_INSTANCES_4)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_3)))
            .operationPreferences(OPERATION_PREFERENCES)
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_NO_INSTANCES_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_ONE_INSTANCES_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_2)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel UPDATED_SELF_MANAGED_ONE_INSTANCES_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_4)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_DUPLICATE_INSTANCES_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_2, SELF_MANAGED_STACK_INSTANCES_4)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_INVALID_INSTANCES_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_2, SERVICE_MANAGED_STACK_INSTANCES_1)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static StackInstances CREATE_STACK_INSTANCES_SELF_MANAGED = StackInstances.builder()
            .deploymentTargets(SELF_MANAGED_TARGETS)
            .regions(new HashSet<>(Arrays.asList(US_EAST_2)))
            .build();

    public final static Queue<StackInstances> CREATE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE = new LinkedList<>(
            Arrays.asList(CREATE_STACK_INSTANCES_SELF_MANAGED));

    public final static StackInstances DELETE_STACK_INSTANCES_SELF_MANAGED = StackInstances.builder()
            .deploymentTargets(SELF_MANAGED_TARGETS)
            .regions(new HashSet<>(Arrays.asList(US_EAST_1)))
            .build();

    public final static Queue<StackInstances> DELETE_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE = new LinkedList<>(
            Arrays.asList(DELETE_STACK_INSTANCES_SELF_MANAGED));

    public final static StackInstances UPDATE_STACK_INSTANCES_SELF_MANAGED = StackInstances.builder()
            .deploymentTargets(UPDATED_SELF_MANAGED_TARGETS)
            .regions(new HashSet<>(Arrays.asList(EU_EAST_1, EU_EAST_2)))
            .parameterOverrides(new HashSet<>(Arrays.asList(PARAMETER_1)))
            .build();

    public final static Queue<StackInstances> UPDATED_STACK_INSTANCES_SELF_MANAGED_FOR_UPDATE = new LinkedList<>(
            Arrays.asList(UPDATE_STACK_INSTANCES_SELF_MANAGED));

    public final static ResourceModel SELF_MANAGED_MODEL_NO_INSTANCES_FOR_READ = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_MODEL_ONE_INSTANCES_FOR_READ = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_1)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_MODEL_FOR_READ = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_1, SELF_MANAGED_STACK_INSTANCES_2)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SERVICE_MANAGED_MODEL_FOR_READ = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SERVICE_MANAGED)
            .autoDeployment(AUTO_DEPLOYMENT_ENABLED)
            .capabilities(CAPABILITIES)
            .templateBody(TEMPLATE_BODY)
            .description(DESCRIPTION)
            .stackInstancesGroup(
                    new HashSet<>(Arrays.asList(SELF_MANAGED_STACK_INSTANCES_1, SELF_MANAGED_STACK_INSTANCES_2)))
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel READ_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .build();

    public final static ResourceModel SIMPLE_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .permissionModel(SELF_MANAGED)
            .templateBody(TEMPLATE_BODY)
            .tags(TAGS)
            .operationPreferences(OPERATION_PREFERENCES)
            .build();

    public final static DescribeStackSetOperationResponse OPERATION_SUCCEED_RESPONSE =
            DescribeStackSetOperationResponse.builder()
                    .stackSetOperation(StackSetOperation.builder()
                            .status(StackSetOperationStatus.SUCCEEDED)
                            .build())
                    .build();

    public final static DescribeStackSetOperationResponse OPERATION_RUNNING_RESPONSE =
            DescribeStackSetOperationResponse.builder()
                    .stackSetOperation(StackSetOperation.builder()
                            .status(StackSetOperationStatus.RUNNING)
                            .build())
                    .build();

    public final static DescribeStackSetOperationResponse OPERATION_STOPPED_RESPONSE =
            DescribeStackSetOperationResponse.builder()
                    .stackSetOperation(StackSetOperation.builder()
                            .status(StackSetOperationStatus.STOPPED)
                            .build())
                    .build();

    public final static CreateStackSetResponse CREATE_STACK_SET_RESPONSE =
            CreateStackSetResponse.builder()
                    .stackSetId(STACK_SET_ID)
                    .build();

    public final static CreateStackInstancesResponse CREATE_STACK_INSTANCES_RESPONSE =
            CreateStackInstancesResponse.builder()
                    .operationId(OPERATION_ID_1)
                    .build();

    public final static DeleteStackSetResponse DELETE_STACK_SET_RESPONSE =
            DeleteStackSetResponse.builder().build();

    public final static UpdateStackSetResponse UPDATE_STACK_SET_RESPONSE =
            UpdateStackSetResponse.builder()
                    .operationId(OPERATION_ID_1)
                    .build();

    public final static UpdateStackInstancesResponse UPDATE_STACK_INSTANCES_RESPONSE =
            UpdateStackInstancesResponse.builder()
                    .operationId(OPERATION_ID_1)
                    .build();

    public final static DeleteStackInstancesResponse DELETE_STACK_INSTANCES_RESPONSE =
            DeleteStackInstancesResponse.builder()
                    .operationId(OPERATION_ID_1)
                    .build();

    public final static DescribeStackSetResponse DESCRIBE_SERVICE_MANAGED_STACK_SET_RESPONSE =
            DescribeStackSetResponse.builder()
                    .stackSet(SERVICE_MANAGED_STACK_SET)
                    .build();

    public final static ListStackInstancesResponse LIST_SERVICE_MANAGED_STACK_SET_RESPONSE =
            ListStackInstancesResponse.builder()
                    .summaries(SERVICE_MANAGED_STACK_INSTANCE_SUMMARIES)
                    .build();

    public final static DescribeStackSetResponse DESCRIBE_SELF_MANAGED_STACK_SET_RESPONSE =
            DescribeStackSetResponse.builder()
                    .stackSet(SELF_MANAGED_STACK_SET)
                    .build();

    public final static DescribeStackSetResponse DESCRIBE_NULL_PERMISSION_MODEL_STACK_SET_RESPONSE =
            DescribeStackSetResponse.builder()
                    .stackSet(NULL_PERMISSION_MODEL_STACK_SET)
                    .build();

    public final static ListStackInstancesResponse LIST_SELF_MANAGED_STACK_SET_RESPONSE =
            ListStackInstancesResponse.builder()
                    .summaries(SELF_MANAGED_STACK_INSTANCE_SUMMARIES)
                    .build();

    public final static ListStackInstancesResponse LIST_SELF_MANAGED_STACK_SET_ONE_INSTANCES_RESPONSE =
            ListStackInstancesResponse.builder()
                    .summaries(SELF_MANAGED_STACK_ONE_INSTANCES_SUMMARIES)
                    .build();

    public final static ListStackInstancesResponse LIST_SELF_MANAGED_STACK_SET_EMPTY_RESPONSE =
            ListStackInstancesResponse.builder()
                    .build();

    public final static ListStackSetsResponse LIST_STACK_SETS_RESPONSE =
            ListStackSetsResponse.builder()
                    .summaries(STACK_SET_SUMMARY_1)
                    .build();

}
