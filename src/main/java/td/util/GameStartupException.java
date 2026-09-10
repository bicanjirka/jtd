package td.util;

/**
 * Thrown when the game cannot start at all - today the intended use is a future file-based
 * {@link td.level.LevelCatalog} failing to read or parse a level file. Caught exactly once,
 * in {@code Main}, which logs it and exits non-zero: the single fatal boundary, rather than
 * each failing component showing its own dialog and calling {@code System.exit} itself.
 */
public class GameStartupException extends RuntimeException {

    public GameStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}
