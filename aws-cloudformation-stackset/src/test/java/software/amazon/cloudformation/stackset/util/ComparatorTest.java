package software.amazon.cloudformation.stackset.util;

import java.util.HashSet;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.Arrays;
import software.amazon.cloudformation.stackset.StackInstances;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.DIFF;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.OU_2;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.account_1;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.generateInstancesWithRegions;
import static software.amazon.cloudformation.stackset.util.AltTestUtils.region_1;
import static software.amazon.cloudformation.stackset.util.Comparator.isAccountLevelTargetingEnabled;
import static software.amazon.cloudformation.stackset.util.Comparator.isStackSetConfigEquals;
import static software.amazon.cloudformation.stackset.util.TestUtils.ADMINISTRATION_ROLE_ARN;
import static software.amazon.cloudformation.stackset.util.TestUtils.AUTO_DEPLOYMENT_DISABLED;
import static software.amazon.cloudformation.stackset.util.TestUtils.AUTO_DEPLOYMENT_ENABLED;
import static software.amazon.cloudformation.stackset.util.TestUtils.AUTO_DEPLOYMENT_ENABLED_COPY;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIPTION;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESIRED_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.EXECUTION_ROLE_NAME;
import static software.amazon.cloudformation.stackset.util.TestUtils.MANAGED_EXECUTION_DISABLED_RESOURCE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.MANAGED_EXECUTION_EMPTY_RESOURCE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL;
import static software.amazon.cloudformation.stackset.util.TestUtils.MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL_COPY;
import static software.amazon.cloudformation.stackset.util.TestUtils.PARAMETER_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.PARAMETER_1_COPY;
import static software.amazon.cloudformation.stackset.util.TestUtils.PARAMETER_1_UPDATED;
import static software.amazon.cloudformation.stackset.util.TestUtils.PARAMETER_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.PREVIOUS_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.SELF_MANAGED;
import static software.amazon.cloudformation.stackset.util.TestUtils.SERVICE_MANAGED;
import static software.amazon.cloudformation.stackset.util.TestUtils.TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.TAGS_TO_UPDATE;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_BODY;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_URL;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_ADMINISTRATION_ROLE_ARN;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_DESCRIPTION;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_EXECUTION_ROLE_NAME;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_TEMPLATE_BODY;
import static software.amazon.cloudformation.stackset.util.TestUtils.UPDATED_TEMPLATE_URL;

public class ComparatorTest {

    @Test
    public void testIsAccountLevelTargetingEnabled() {
        assertThat(isAccountLevelTargetingEnabled(null)).isFalse();
        assertThat(isAccountLevelTargetingEnabled(ResourceModel.builder().permissionModel(SELF_MANAGED).build())).isFalse();
        assertThat(isAccountLevelTargetingEnabled(ResourceModel.builder().permissionModel(SERVICE_MANAGED).build())).isFalse();

        assertThat(isAccountLevelTargetingEnabled(ResourceModel.builder()
                .permissionModel(SERVICE_MANAGED)
                .stackInstancesGroup(new HashSet<>())
                .build())).isFalse();

        assertThat(isAccountLevelTargetingEnabled(ResourceModel.builder()
                .permissionModel(SERVICE_MANAGED)
                .stackInstancesGroup(new HashSet<>(Arrays.asList(
                        StackInstances.builder().build()
                )))
                .build())).isFalse();

        assertThat(isAccountLevelTargetingEnabled(ResourceModel.builder()
                .permissionModel(SERVICE_MANAGED)
                .stackInstancesGroup(new HashSet<>(Arrays.asList(
                        generateInstancesWithRegions(OU_1, region_1)
                )))
                .build())).isFalse();

        assertThat(isAccountLevelTargetingEnabled(ResourceModel.builder()
                .permissionModel(SERVICE_MANAGED)
                .stackInstancesGroup(new HashSet<>(Arrays.asList(
                        generateInstancesWithRegions(OU_1, region_1),
                        generateInstancesWithRegions(OU_1, account_1, DIFF, region_1)
                )))
                .build())).isTrue();
    }

    @Test
    public void testIsStackSetConfigEquals() {

        final ResourceModel testPreviousModel = ResourceModel.builder().build();
        final ResourceModel testDesiredModel = ResourceModel.builder().build();

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, PREVIOUS_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        testDesiredModel.setAdministrationRoleARN(UPDATED_ADMINISTRATION_ROLE_ARN);
        testPreviousModel.setAdministrationRoleARN(ADMINISTRATION_ROLE_ARN);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        testDesiredModel.setAdministrationRoleARN(ADMINISTRATION_ROLE_ARN);
        testDesiredModel.setDescription(UPDATED_DESCRIPTION);
        testPreviousModel.setDescription(DESCRIPTION);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        testDesiredModel.setDescription(DESCRIPTION);
        testDesiredModel.setExecutionRoleName(UPDATED_EXECUTION_ROLE_NAME);
        testPreviousModel.setExecutionRoleName(EXECUTION_ROLE_NAME);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        testDesiredModel.setExecutionRoleName(EXECUTION_ROLE_NAME);
        // Different parameters key/value pairs should not equal
        testDesiredModel.setParameters(Sets.newHashSet(Arrays.asList(PARAMETER_1, PARAMETER_2)));
        testPreviousModel.setParameters(Sets.newHashSet(Arrays.asList(PARAMETER_1_UPDATED, PARAMETER_2)));

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        // Same parameters key/value pairs should equal
        testDesiredModel.setParameters(Sets.newHashSet(Arrays.asList(PARAMETER_1, PARAMETER_2)));
        testPreviousModel.setParameters(Sets.newHashSet(Arrays.asList(PARAMETER_1_COPY, PARAMETER_2)));

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isTrue();

        // Testing AutoDeployment objects not equal
        testDesiredModel.setAutoDeployment(AUTO_DEPLOYMENT_DISABLED);
        testPreviousModel.setAutoDeployment(AUTO_DEPLOYMENT_ENABLED);
        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        // Testing AutoDeployment objects equal
        testDesiredModel.setAutoDeployment(AUTO_DEPLOYMENT_ENABLED);
        testPreviousModel.setAutoDeployment(AUTO_DEPLOYMENT_ENABLED_COPY);
        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isTrue();

        testDesiredModel.setTemplateURL(UPDATED_TEMPLATE_URL);
        testPreviousModel.setTemplateURL(TEMPLATE_URL);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        // Even if both TemplateURLs remain no change, we still need to call Update API
        // The service client will decide if it needs to update
        testDesiredModel.setTemplateURL(TEMPLATE_URL);
        testPreviousModel.setTemplateURL(TEMPLATE_URL);
        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        // previously using TemplateURL, currently using TemplateBody
        testPreviousModel.setTemplateURL(TEMPLATE_URL);
        testDesiredModel.setTemplateURL(null);
        testDesiredModel.setTemplateBody(TEMPLATE_BODY);
        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        // previously using TemplateBody, currently using TemplateURL
        testPreviousModel.setTemplateBody(TEMPLATE_URL);
        testPreviousModel.setTemplateURL(null);
        testDesiredModel.setTemplateBody(null);
        testDesiredModel.setTemplateURL(TEMPLATE_URL);
        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        // Both using TemplateBody
        testDesiredModel.setTemplateURL(null);
        testPreviousModel.setTemplateURL(null);

        testDesiredModel.setTemplateBody(UPDATED_TEMPLATE_BODY);
        testPreviousModel.setTemplateBody(TEMPLATE_BODY);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isFalse();

        testDesiredModel.setTemplateBody(TEMPLATE_BODY);
        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel, DESIRED_RESOURCE_TAGS, DESIRED_RESOURCE_TAGS)).isTrue();

    }

    @Test
    public void testIsStackSetConfigManagedExecutionEquals() {
        // Testing ManagedExecution objects not equal
        assertThat(isStackSetConfigEquals(MANAGED_EXECUTION_DISABLED_RESOURCE_MODEL, MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL)).isFalse();
        // Testing ManagedExecution objects equal
        assertThat(isStackSetConfigEquals(MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL, MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL)).isTrue();
        assertThat(isStackSetConfigEquals(MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL, MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL_COPY)).isTrue();
        assertThat(isStackSetConfigEquals(MANAGED_EXECUTION_EMPTY_RESOURCE_MODEL, MANAGED_EXECUTION_DISABLED_RESOURCE_MODEL)).isTrue();
        assertThat(isStackSetConfigEquals(MANAGED_EXECUTION_DISABLED_RESOURCE_MODEL, MANAGED_EXECUTION_EMPTY_RESOURCE_MODEL)).isTrue();
        assertThat(isStackSetConfigEquals(MANAGED_EXECUTION_DISABLED_RESOURCE_MODEL, null)).isTrue();
        assertThat(isStackSetConfigEquals(null, MANAGED_EXECUTION_DISABLED_RESOURCE_MODEL)).isTrue();
    }

    @Test
    public void testEquals() {
        assertThat(Comparator.equals(TAGS, null)).isFalse();
        assertThat(Comparator.equals(null, TAGS)).isFalse();
        assertThat(Comparator.equals(TAGS, TAGS)).isTrue();
        assertThat(Comparator.equals(TAGS, TAGS_TO_UPDATE)).isFalse();
        assertThat(Comparator.equals(DESIRED_RESOURCE_TAGS, null)).isFalse();
        assertThat(Comparator.equals(null, DESIRED_RESOURCE_TAGS)).isFalse();
        assertThat(Comparator.equals(null, null)).isTrue();
        assertThat(Comparator.equals(null, AUTO_DEPLOYMENT_ENABLED)).isFalse();
        assertThat(Comparator.equals(AUTO_DEPLOYMENT_ENABLED, null)).isFalse();
        assertThat(Comparator.equals(Arrays.asList(PARAMETER_1, PARAMETER_2), Arrays.asList(PARAMETER_1))).isFalse();
    }

}
