package software.amazon.cloudformation.stackset.translator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkTags;

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
}
