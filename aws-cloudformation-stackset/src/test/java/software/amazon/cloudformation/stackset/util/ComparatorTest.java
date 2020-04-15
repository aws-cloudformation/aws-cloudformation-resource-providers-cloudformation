package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.stackset.CallbackContext;
import software.amazon.cloudformation.stackset.ResourceModel;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.util.Comparator.isAddingStackInstances;
import static software.amazon.cloudformation.stackset.util.Comparator.isDeletingStackInstances;
import static software.amazon.cloudformation.stackset.util.Comparator.isEquals;
import static software.amazon.cloudformation.stackset.util.Comparator.isStackSetConfigEquals;
import static software.amazon.cloudformation.stackset.util.Comparator.isUpdatingStackInstances;
import static software.amazon.cloudformation.stackset.util.TestUtils.ADMINISTRATION_ROLE_ARN;
import static software.amazon.cloudformation.stackset.util.TestUtils.DESCRIPTION;
import static software.amazon.cloudformation.stackset.util.TestUtils.EXECUTION_ROLE_NAME;
import static software.amazon.cloudformation.stackset.util.TestUtils.REGIONS;
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

        final ResourceModel testPreviousModel = ResourceModel.builder().tags(TAGS).build();
        final ResourceModel testDesiredModel = ResourceModel.builder().tags(TAGS_TO_UPDATE).build();

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel)).isFalse();

        testDesiredModel.setTags(TAGS);
        testDesiredModel.setAdministrationRoleARN(UPDATED_ADMINISTRATION_ROLE_ARN);
        testPreviousModel.setAdministrationRoleARN(ADMINISTRATION_ROLE_ARN);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel)).isFalse();

        testDesiredModel.setAdministrationRoleARN(ADMINISTRATION_ROLE_ARN);
        testDesiredModel.setDescription(UPDATED_DESCRIPTION);
        testPreviousModel.setDescription(DESCRIPTION);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel)).isFalse();

        testDesiredModel.setDescription(DESCRIPTION);
        testDesiredModel.setExecutionRoleName(UPDATED_EXECUTION_ROLE_NAME);
        testPreviousModel.setExecutionRoleName(EXECUTION_ROLE_NAME);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel)).isFalse();

        testDesiredModel.setExecutionRoleName(EXECUTION_ROLE_NAME);
        testDesiredModel.setTemplateURL(UPDATED_TEMPLATE_URL);
        testPreviousModel.setTemplateURL(TEMPLATE_URL);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel)).isFalse();

        testDesiredModel.setTemplateURL(null);
        testPreviousModel.setTemplateURL(null);

        testDesiredModel.setTemplateBody(UPDATED_TEMPLATE_BODY);
        testPreviousModel.setTemplateBody(TEMPLATE_BODY);

        assertThat(isStackSetConfigEquals(testPreviousModel, testDesiredModel)).isFalse();
    }

    @Test
    public void testIsDeletingStackInstances() {
        // Both are empty
        assertThat(isDeletingStackInstances(new HashSet<>(), new HashSet<>(), CallbackContext.builder().build()))
                .isFalse();
        // targetsToDelete is empty
        assertThat(isDeletingStackInstances(REGIONS, new HashSet<>(), CallbackContext.builder().build()))
                .isTrue();
    }

    @Test
    public void testisAddingStackInstances() {
        // Both are empty
        assertThat(isAddingStackInstances(new HashSet<>(), new HashSet<>(), CallbackContext.builder().build()))
                .isFalse();
        // targetsToDelete is empty
        assertThat(isAddingStackInstances(REGIONS, new HashSet<>(), CallbackContext.builder().build()))
                .isTrue();
    }

    @Test
    public void testIsEquals() {
        assertThat(isEquals(null, TAGS)).isFalse();
    }

}
