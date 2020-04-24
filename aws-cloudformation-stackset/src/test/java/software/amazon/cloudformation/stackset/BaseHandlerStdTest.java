package software.amazon.cloudformation.stackset;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.translator.PropertyTranslator.translateFromSdkTags;

public class BaseHandlerStdTest {
    @Test
    public void test_translateFromSdkTags_IfIsNull() {
        assertThat(translateFromSdkTags(null)).isNull();
    }
}
