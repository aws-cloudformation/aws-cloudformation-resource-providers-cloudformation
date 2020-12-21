package software.amazon.cloudformation.stackset.translator;

import org.junit.jupiter.api.Test;
import software.amazon.cloudformation.stackset.AutoDeployment;
import software.amazon.cloudformation.stackset.util.StackInstance;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkAutoDeployment;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToStackInstance;
import static software.amazon.cloudformation.stackset.util.TestUtils.EU_EAST_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.ORGANIZATION_UNIT_ID_2;
import static software.amazon.cloudformation.stackset.util.TestUtils.PARAMETER_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.SDK_AUTO_DEPLOYMENT;
import static software.amazon.cloudformation.stackset.util.TestUtils.SDK_PARAMETER_1;
import static software.amazon.cloudformation.stackset.util.TestUtils.STACK_INSTANCE_SUMMARY_4;

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
        final AutoDeployment autoDeployment = translateFromSdkAutoDeployment(SDK_AUTO_DEPLOYMENT);
        assertThat(autoDeployment.getEnabled()).isTrue();
        assertThat(autoDeployment.getRetainStacksOnAccountRemoval()).isTrue();
    }

    @Test
    public void test_translateToStackInstance() {
        final StackInstance stackInstance = translateToStackInstance(false, STACK_INSTANCE_SUMMARY_4, Arrays.asList(SDK_PARAMETER_1));
        assertThat(stackInstance.getRegion()).isEqualTo(EU_EAST_2);
        assertThat(stackInstance.getDeploymentTarget()).isEqualTo(ORGANIZATION_UNIT_ID_2);
        assertThat(stackInstance.getParameters()).contains(PARAMETER_1);
    }
}
