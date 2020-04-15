package software.amazon.cloudformation.stackset.util;

import com.google.common.collect.ImmutableMap;
import software.amazon.awssdk.services.cloudformation.model.CreateStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.CreateStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetOperationResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackSetResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackInstancesResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackSetsResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.PermissionModels;
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            .append("  \"AWSTemplateFormatVersion\" : \"2010-09-09\",\n" )
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
            .append("  \"AWSTemplateFormatVersion\" : \"2010-09-09\",\n" )
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

    public final static String OPERATION_ID_2 = "operation-id-2";
    public final static String OPERATION_ID_3 = "operation-id-3";
    public final static String OPERATION_ID_4 = "operation-id-4";
    public final static String OPERATION_ID_5 = "operation-id-5";

    public final static String LOGICAL_ID = "MyResource";
    public final static String REQUEST_TOKEN = "token";

    public final static String SERVICE_MANAGED = "SERVICE_MANAGED";
    public final static String SELF_MANAGED = "SELF_MANAGED";

    public final static String US_EAST_1 = "us-east-1";
    public final static String US_WEST_1 = "us-west-1";
    public final static String US_EAST_2 = "us-east-2";
    public final static String US_WEST_2 = "us-west-2";

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

    public final static Map<String, Object> TEMPLATE_MAP = ImmutableMap.of("TemplateURL", "foo");

    public final static Map<String, String> DESIRED_RESOURCE_TAGS = ImmutableMap.of(
            "key1", "val1", "key2", "val2", "key3", "val3");
    public final static Map<String, String> PREVIOUS_RESOURCE_TAGS = ImmutableMap.of(
            "key-1", "val1", "key-2", "val2", "key-3", "val3");
    public final static Map<String, String> NEW_RESOURCE_TAGS = ImmutableMap.of(
            "key1", "val1", "key2updated", "val2updated", "key3", "val3");

    public final static Set<String> REGIONS = new HashSet<>(Arrays.asList(US_WEST_1, US_EAST_1));
    public final static Set<String> UPDATED_REGIONS = new HashSet<>(Arrays.asList(US_WEST_2, US_EAST_2));

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
                    ACCOUNT_ID_1, ACCOUNT_ID_2)))
            .build();

    public final static DeploymentTargets UPDATED_SELF_MANAGED_TARGETS = DeploymentTargets.builder()
            .accounts(new HashSet<>(Arrays.asList(
                    ACCOUNT_ID_3, ACCOUNT_ID_4)))
            .build();

    public final static Set<String> CAPABILITIES = new HashSet<>(Arrays.asList(
            "CAPABILITY_IAM", "CAPABILITY_NAMED_IAM"));

    public final static OperationPreferences OPERATION_PREFERENCES = OperationPreferences.builder()
            .failureToleranceCount(0)
            .maxConcurrentCount(1)
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

    public final static AutoDeployment AUTO_DEPLOYMENT = AutoDeployment.builder()
            .enabled(true)
            .retainStacksOnAccountRemoval(true)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_1 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_1)
            .region(US_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_2 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_1)
            .region(US_WEST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_3 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_2)
            .region(US_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_4 = StackInstanceSummary.builder()
            .organizationalUnitId(ORGANIZATION_UNIT_ID_2)
            .region(US_WEST_1)
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
            .region(US_EAST_1)
            .build();

    public final static StackInstanceSummary STACK_INSTANCE_SUMMARY_8 = StackInstanceSummary.builder()
            .account(ACCOUNT_ID_2)
            .region(US_WEST_1)
            .build();


    public final static List<StackInstanceSummary> SERVICE_MANAGED_STACK_INSTANCE_SUMMARIES = Arrays.asList(
            STACK_INSTANCE_SUMMARY_1, STACK_INSTANCE_SUMMARY_2, STACK_INSTANCE_SUMMARY_3, STACK_INSTANCE_SUMMARY_4);

    public final static List<StackInstanceSummary> SERVICE_SELF_STACK_INSTANCE_SUMMARIES = Arrays.asList(
            STACK_INSTANCE_SUMMARY_5, STACK_INSTANCE_SUMMARY_6, STACK_INSTANCE_SUMMARY_7, STACK_INSTANCE_SUMMARY_8);

    public final static software.amazon.awssdk.services.cloudformation.model.AutoDeployment SDK_AUTO_DEPLOYMENT =
            software.amazon.awssdk.services.cloudformation.model.AutoDeployment.builder()
                    .retainStacksOnAccountRemoval(true)
                    .enabled(true)
                    .build();

    public final static StackSetSummary STACK_SET_SUMMARY_1 = StackSetSummary.builder()
            .autoDeployment(SDK_AUTO_DEPLOYMENT)
            .description(DESCRIPTION)
            .permissionModel(PermissionModels.SERVICE_MANAGED)
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
            .tags(TAGGED_RESOURCES)
            .build();

    public final static StackSet SELF_MANAGED_STACK_SET = StackSet.builder()
            .stackSetId(STACK_SET_ID)
            .stackSetName(STACK_SET_NAME)
            .capabilitiesWithStrings(CAPABILITIES)
            .description(DESCRIPTION)
            .parameters(SDK_PARAMETER_1, SDK_PARAMETER_2)
            .permissionModel(PermissionModels.SELF_MANAGED)
            .tags(TAGGED_RESOURCES)
            .build();

    public final static ResourceModel SERVICE_MANAGED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .deploymentTargets(SERVICE_MANAGED_TARGETS)
            .permissionModel(SERVICE_MANAGED)
            .capabilities(CAPABILITIES)
            .description(DESCRIPTION)
            .autoDeployment(AUTO_DEPLOYMENT)
            .regions(REGIONS)
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel SELF_MANAGED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .deploymentTargets(SELF_MANAGED_TARGETS)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .description(DESCRIPTION)
            .regions(REGIONS)
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_2)))
            .tags(TAGS)
            .build();

    public final static ResourceModel UPDATED_SELF_MANAGED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .deploymentTargets(UPDATED_SELF_MANAGED_TARGETS)
            .permissionModel(SELF_MANAGED)
            .capabilities(CAPABILITIES)
            .regions(UPDATED_REGIONS)
            .parameters(new HashSet<>(Arrays.asList(PARAMETER_1, PARAMETER_3)))
            .tags(TAGS)
            .build();

    public final static ResourceModel READ_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .build();

    public final static ResourceModel SIMPLE_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .deploymentTargets(SERVICE_MANAGED_TARGETS)
            .permissionModel(SERVICE_MANAGED)
            .autoDeployment(AUTO_DEPLOYMENT)
            .regions(REGIONS)
            .templateURL(TEMPLATE_URL)
            .tags(TAGS)
            .operationPreferences(OPERATION_PREFERENCES)
            .build();

    public final static ResourceModel SIMPLE_TEMPLATE_BODY_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .deploymentTargets(SERVICE_MANAGED_TARGETS)
            .permissionModel(SERVICE_MANAGED)
            .autoDeployment(AUTO_DEPLOYMENT)
            .regions(REGIONS)
            .templateBody(TEMPLATE_BODY)
            .tags(TAGS)
            .operationPreferences(OPERATION_PREFERENCES)
            .build();


    public final static ResourceModel UPDATED_MODEL = ResourceModel.builder()
            .stackSetId(STACK_SET_ID)
            .deploymentTargets(UPDATED_SERVICE_MANAGED_TARGETS)
            .permissionModel(SERVICE_MANAGED)
            .autoDeployment(AUTO_DEPLOYMENT)
            .regions(UPDATED_REGIONS)
            .templateURL(UPDATED_TEMPLATE_URL)
            .tags(TAGS_TO_UPDATE)
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

    public final static ListStackInstancesResponse LIST_SELF_MANAGED_STACK_SET_RESPONSE =
            ListStackInstancesResponse.builder()
                    .summaries(SERVICE_SELF_STACK_INSTANCE_SUMMARIES)
                    .build();

    public final static ListStackSetsResponse LIST_STACK_SETS_RESPONSE =
            ListStackSetsResponse.builder()
                    .summaries(STACK_SET_SUMMARY_1)
                    .build();

}