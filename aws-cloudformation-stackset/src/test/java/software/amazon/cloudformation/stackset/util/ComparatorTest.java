package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.stackset.ResourceModel;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.util.Comparator.isStackSetConfigEquals;
import static software.amazon.cloudformation.stackset.util.TestUtils.ADMINISTRATION_ROLE_ARN;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIPTION;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESIRED_RESOURCE_TAGS;
import static software.amazon.cloudformation.stackset.util.TestUtils.EXECUTION_ROLE_NAME;
import static software.amazon.cloudformation.stackset.util.TestUtils.PREVIOUS_RESOURCE_TAGS;
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
    public void testEquals() {
        assertThat(Comparator.equals(TAGS, null)).isFalse();
        assertThat(Comparator.equals(null, TAGS)).isFalse();
        assertThat(Comparator.equals(TAGS, TAGS)).isTrue();
        assertThat(Comparator.equals(TAGS, TAGS_TO_UPDATE)).isFalse();
        assertThat(Comparator.equals(DESIRED_RESOURCE_TAGS, null)).isFalse();
        assertThat(Comparator.equals(null, DESIRED_RESOURCE_TAGS)).isFalse();
    }

}
