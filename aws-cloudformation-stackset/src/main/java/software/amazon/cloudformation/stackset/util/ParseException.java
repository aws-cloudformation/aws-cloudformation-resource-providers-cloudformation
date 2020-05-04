package software.amazon.cloudformation.stackset.util;

/**
 * Custom Exception Class to hold exception when parsing templates
 */
public class ParseException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ParseException(final String message) {
        super(message);
    }
}
