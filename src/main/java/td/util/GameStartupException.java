package td.util;

/**
 * The game cannot start, e.g. unloadable content. Caught only in {@code Main}, which logs it and
 * exits non-zero.
 */
public class GameStartupException extends RuntimeException {

    public GameStartupException(String message, Throwable cause) {
        super(message, cause);
    }

    public GameStartupException(String message) {
        this(message, null);
    }
}
