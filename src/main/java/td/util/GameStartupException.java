package td.util;

/**
 * Thrown when the game cannot start at all (e.g. a required image resource
 * failed to load). Caught exactly once, in {@code Main}, which logs it and
 * exits - the single fatal boundary rather than a singleton constructor
 * showing a dialog and calling {@code System.exit} itself.
 */
public class GameStartupException extends RuntimeException {

    public GameStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
