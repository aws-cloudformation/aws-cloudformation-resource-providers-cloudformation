package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.cloudformation.stackset.util.TemplateParser.formatTemplateErrorLocation;
import static software.amazon.cloudformation.stackset.util.TemplateParser.getMapFromTemplate;
import static software.amazon.cloudformation.stackset.util.TemplateParser.getStringFromTemplate;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_MAP;

public class TemplateParserTest {

    private static final String UNKNOWN_LOCATION = "unknown location";

    @Test
    public void testDeserializeYaml() {
        assertThrows(ParseException.class, () -> TemplateParser.deserializeTemplate("null"));
        assertThrows(ParseException.class, () -> TemplateParser.deserializeTemplate(""));
    }

    @Test
    public void testGetMapFromTemplate() {
        assertThat(getMapFromTemplate(TEMPLATE_MAP, "null")).isNull();
        assertThrows(ParseException.class, () -> getMapFromTemplate(TEMPLATE_MAP, "TemplateURL"));
    }

    @Test
    public void testGetStringFromTemplate() {
        assertThat(getStringFromTemplate(null)).isNull();
        assertThrows(ParseException.class, () -> getStringFromTemplate(TEMPLATE_MAP));
    }

    @Test
    public void testFormatErrorLocation_IfIsNull() {
        assertThat(formatTemplateErrorLocation(null)).isEqualTo(UNKNOWN_LOCATION);
    }
}
