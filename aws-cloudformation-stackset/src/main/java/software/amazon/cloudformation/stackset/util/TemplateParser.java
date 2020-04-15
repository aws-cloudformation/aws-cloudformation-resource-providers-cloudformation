package software.amazon.cloudformation.stackset.util;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class TemplateParser {

    private static final Pattern JSON_INPUT_PATTERN = Pattern.compile("^\\s*\\{.*\\}\\s*$", Pattern.DOTALL);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String UNKNOWN_LOCATION = "unknown location";
    private static final String INVALID_TEMPLATE_ERROR_MSG = "Template format error: not a valid template";
    private static final String UNSUPPORTED_TYPE_STRUCTURE_ERROR_MSG =
            "Template format error: unsupported type or structure. (%s)";
    private static final String NOT_WELL_FORMATTED_ERROR_MSG = "Template format error: %s not well-formed. (%s)";
    private static final String FORMAT_LOCATION_ERROR_MSG = "line %s, column %s";

    /**
     * Deserializes template content which can be either JSON or YAML
     * @param template Template Content
     * @return Generic Map of template
     */
    public static Map<String, Object> deserializeTemplate(final String template) {
        // If the template does not follow valid Json pattern, parse as Yaml.
        // Else, parse as Json first; if that fails parse as Yaml.
        if (!isPossiblyJson(template)) {
            return deserializeYaml(template);
        }

        try {
            return deserializeJson(template);
        } catch (final ParseException e) {
            return deserializeYaml(template);
        }

    }

    /**
     * Gets a Generic Map object from template
     * @param templateMap Template Map
     * @param key Key of the Map we are retrieving
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
     * Deserializes YAML from template content string
     * @param templateString Template content
     * @return Template map
     * @throws ParseException if fails to parse the template
     */
    @VisibleForTesting
    protected static Map<String, Object> deserializeYaml(final String templateString) {
        try {
            final Map<String, Object> template = new Yaml().load(templateString);
            if (template == null || template.isEmpty()) {
                throw new ParseException(INVALID_TEMPLATE_ERROR_MSG);
            }
            return template;

        } catch (final MarkedYAMLException e) {
            throw new ParseException(String.format(NOT_WELL_FORMATTED_ERROR_MSG, "YAML",
                    formatErrorLocation(e.getProblemMark())));

        } catch (final YAMLException e) {
            throw new ParseException(String.format("Cannot parse as YAML : %s ", e.getMessage()));

        } catch (final ClassCastException e) {
            throw new ParseException("Template format error: unsupported structure.");

        }
    }

    /**
     * Deserializes JSON from template content string
     * @param templateString Template content
     * @return Template map
     * @throws ParseException if fails to parse the template
     */
    @SuppressWarnings("unchecked")
    @VisibleForTesting
    protected static Map<String, Object> deserializeJson(final String templateString) {
        Map<String, Object> template = null;
        try {
            JsonParser parser = new MappingJsonFactory().createParser(templateString);
            template = OBJECT_MAPPER.readValue(parser, Map.class);

        } catch (final JsonMappingException e) {
            throw new ParseException(String.format(UNSUPPORTED_TYPE_STRUCTURE_ERROR_MSG,
                    formatErrorLocation(e.getLocation())));

        } catch (final JsonParseException e) {
            throw new ParseException(String.format(NOT_WELL_FORMATTED_ERROR_MSG, "JSON",
                    formatErrorLocation(e.getLocation())));

        } catch (final IOException e) {
            throw new ParseException("Cannot parse template, I/O stream corrupt.");
        }

        // The string "null" may be considered as valid JSON by the parser, but it is not a valid template.
        if (template == null) {
            throw new ParseException(INVALID_TEMPLATE_ERROR_MSG);
        }
        return template;
    }

    private static boolean isPossiblyJson(final String template) {
        return JSON_INPUT_PATTERN.matcher(template).matches();
    }

    /**
     * Gets the error location when parsing as JSON
     * @param loc {@link JsonLocation}
     * @return Error location
     */
    private static String formatErrorLocation(final JsonLocation loc) {
        if (loc == null) return UNKNOWN_LOCATION;
        return String.format(FORMAT_LOCATION_ERROR_MSG, loc.getLineNr(), loc.getColumnNr());
    }

    /**
     * Gets the error location when parsing as YAML
     * @param loc {@link Mark}
     * @return Error location
     */
    private static String formatErrorLocation(final Mark loc) {
        if (loc == null) return UNKNOWN_LOCATION;
        return String.format(FORMAT_LOCATION_ERROR_MSG, loc.getLine() + 1, loc.getColumn() + 1);
    }
}
