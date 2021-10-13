package software.amazon.cloudformation.stackset.translator;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudformation.model.RegionConcurrencyType;
import software.amazon.awssdk.services.cloudformation.model.StackSetOperationPreferences;
import software.amazon.awssdk.services.cloudformation.model.ManagedExecution;
import software.amazon.cloudformation.stackset.AutoDeployment;
import software.amazon.cloudformation.stackset.util.StackInstance;
import software.amazon.cloudformation.stackset.util.TestUtils;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkAutoDeployment;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkManagedExecution;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkOperationPreferences;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToStackInstance;
import static software.amazon.cloudformation.stackset.util.TestUtils.EU_EAST_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.ORGANIZATION_UNIT_ID_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.PARAMETER_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.SDK_AUTO_DEPLOYMENT_ENABLED;
import static software.amazon.cloudformation.stackset.util.TestUtils.SDK_PARAMETER_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.STACK_INSTANCE_SUMMARY_OU2_LHR;

public class PropertyTranslatorTest {

    @Test
    public void test_translateFromSdkParameters_IfIsNull() {
        assertThat(translateFromSdkParameters(null)).isNull();
    }

    @Test
    public void test_translateToSdkTags_IfIsNull() {
        assertThat(translateToSdkTags(null)).isEmpty();
    }

    @Test
    public void test_translateFromSdkTags_IfIsNull() {
        assertThat(translateFromSdkTags(null)).isNull();
    }

    @Test
    public void test_translateFromSdkAutoDeployment() {
        final AutoDeployment autoDeployment = translateFromSdkAutoDeployment(SDK_AUTO_DEPLOYMENT_ENABLED);
        assertThat(autoDeployment.getEnabled()).isTrue();
        assertThat(autoDeployment.getRetainStacksOnAccountRemoval()).isTrue();
    }

    @Test
    public void test_translateToStackInstance() {
        final StackInstance stackInstance = translateToStackInstance(false, STACK_INSTANCE_SUMMARY_OU2_LHR, Arrays.asList(SDK_PARAMETER_1));
        assertThat(stackInstance.getRegion()).isEqualTo(EU_EAST_2);
        assertThat(stackInstance.getDeploymentTarget()).isEqualTo(ORGANIZATION_UNIT_ID_2);
        assertThat(stackInstance.getParameters()).contains(PARAMETER_1);
    }

    @Test
    public void test_translateToStackSetOperationPreferences() {
        final StackSetOperationPreferences stackSetOperationPreferences = translateToSdkOperationPreferences(TestUtils.OPERATION_PREFERENCES_FULL);
        assertThat(stackSetOperationPreferences.failureToleranceCount()).isEqualTo(0);
        assertThat(stackSetOperationPreferences.failureTolerancePercentage()).isEqualTo(0);
        assertThat(stackSetOperationPreferences.maxConcurrentCount()).isEqualTo(1);
        assertThat(stackSetOperationPreferences.maxConcurrentPercentage()).isEqualTo(100);
        assertThat(stackSetOperationPreferences.regionOrder()).isEqualTo(Arrays.asList(TestUtils.US_WEST_1, TestUtils.US_EAST_1));
        assertThat(stackSetOperationPreferences.regionConcurrencyType()).isEqualTo(RegionConcurrencyType.PARALLEL);
    }

    @Test
    public void test_translateToManagedExecution() {
        final ManagedExecution managedExecution = translateToSdkManagedExecution(TestUtils.MANAGED_EXECUTION_ENABLED_RESOURCE_MODEL);
        assertThat(managedExecution.active()).isEqualTo(true);
    }
}
