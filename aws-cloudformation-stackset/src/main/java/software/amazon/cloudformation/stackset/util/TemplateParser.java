package software.amazon.cloudformation.stackset.util;

import com.google.common.annotations.VisibleForTesting;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.Map;

public class TemplateParser {

    private static final String UNKNOWN_LOCATION = "unknown location";
    private static final String INVALID_TEMPLATE_ERROR_MSG = "Template format error: not a valid template";
    private static final String NOT_WELL_FORMATTED_ERROR_MSG = "Template format error: not well-formed. (%s)";
    private static final String FORMAT_LOCATION_ERROR_MSG = "line %s, column %s";

    /**
     * Gets a Generic Map object from template
     *
     * @param templateMap Template Map
     * @param key         Key of the Map we are retrieving
     * @return Generic Map object
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMapFromTemplate(final Map<String, Object> templateMap, final String key) {
        final Object value = templateMap.get(key);
        if (value == null) return null;
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        throw new ParseException(INVALID_TEMPLATE_ERROR_MSG);
    }

    /**
     * Gets String from the passed in value
     *
     * @param value
     * @return String
     */
    public static String getStringFromTemplate(final Object value) {
        if (value == null) return null;
        if (value instanceof String) {
            return (String) value;
        }
        throw new ParseException(INVALID_TEMPLATE_ERROR_MSG);
    }

    /**
     * Deserializes YAML/JSON from template content string,
     * Since Yaml is a superset of JSON, we are parsing both with YAML library.
     *
     * @param templateString Template content
     * @return Template map
     * @throws ParseException if fails to parse the template
     */
    public static Map<String, Object> deserializeTemplate(final String templateString) {
        try {
            final Map<String, Object> template = new Yaml(new TemplateConstructor()).load(templateString);
            if (template == null || template.isEmpty()) {
                throw new ParseException(INVALID_TEMPLATE_ERROR_MSG);
            }
            return template;

        } catch (final MarkedYAMLException e) {
            throw new ParseException(String.format(NOT_WELL_FORMATTED_ERROR_MSG,
                    formatTemplateErrorLocation(e.getProblemMark())));

        } catch (final YAMLException e) {
            throw new ParseException(String.format("Cannot parse the template : %s ", e.getMessage()));

        } catch (final ClassCastException e) {
            throw new ParseException("Template format error: unsupported structure.");

        }
    }

    /**
     * Gets the error location when parsing as YAML
     *
     * @param loc {@link Mark}
     * @return Error location
     */
    @VisibleForTesting
    protected static String formatTemplateErrorLocation(final Mark loc) {
        if (loc == null) return UNKNOWN_LOCATION;
        return String.format(FORMAT_LOCATION_ERROR_MSG, loc.getLine() + 1, loc.getColumn() + 1);
    }
}
