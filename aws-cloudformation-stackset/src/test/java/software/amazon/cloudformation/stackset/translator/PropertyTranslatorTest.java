package software.amazon.cloudformation.stackset.translator;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkParameters;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateToSdkTags;

public class PropertyTranslatorTest {

    @Test
    public void testNull_translateFromSdkParameters_isNull() {
        assertThat(translateFromSdkParameters(null)).isNull();
    }

    @Test
    public void test_translateToSdkTags_isNull() {
        assertThat(translateToSdkTags(null)).isNull();
    }

    @Test
    public void test_translateFromSdkTags_isNull() {
        assertThat(translateFromSdkTags(null)).isNull();
    }
}
