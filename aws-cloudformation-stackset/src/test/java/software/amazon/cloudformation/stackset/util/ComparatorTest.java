package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.cloudformation.stackset.util.Comparator.isEquals;
import static software.amazon.cloudformation.stackset.util.TestUtils.TAGS;

public class ComparatorTest {

    @Test
    public void testIsEquals() {
        assertThat(isEquals(null, TAGS)).isFalse();
    }

}
