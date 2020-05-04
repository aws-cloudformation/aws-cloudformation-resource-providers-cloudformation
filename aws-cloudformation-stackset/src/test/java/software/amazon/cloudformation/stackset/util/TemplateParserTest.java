package software.amazon.cloudformation.stackset.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static software.amazon.cloudformation.stackset.util.TemplateParser.formatJsonErrorLocation;
import static software.amazon.cloudformation.stackset.util.TemplateParser.formatYamlErrorLocation;
import static software.amazon.cloudformation.stackset.util.TemplateParser.getMapFromTemplate;
import static software.amazon.cloudformation.stackset.util.TemplateParser.getStringFromTemplate;
import static software.amazon.cloudformation.stackset.util.TestUtils.TEMPLATE_MAP;

public class TemplateParserTest {

    private static final String UNKNOWN_LOCATION = "unknown location";

    @Test
    public void testDeserializeYaml() {
        assertThrows(ParseException.class, () -> TemplateParser.deserializeYaml("null"));
        assertThrows(ParseException.class, () -> TemplateParser.deserializeYaml(""));
    }

    @Test
    public void testDeserializeJson() {
        assertThrows(ParseException.class, () -> TemplateParser.deserializeJson(""));
        assertThrows(ParseException.class, () -> TemplateParser.deserializeJson("null"));
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
        assertThat(formatYamlErrorLocation(null)).isEqualTo(UNKNOWN_LOCATION);
        assertThat(formatJsonErrorLocation(null)).isEqualTo(UNKNOWN_LOCATION);
    }
}
