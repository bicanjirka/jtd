package td.term;

/**
 * A named thing of the game: an effect, a tower buff or mechanic, an enemy trait or ability. Each has one title the
 * player reads everywhere and one sentence that says what it does.
 */
public interface Term {

    /** Unique across all terms and stable, lower case: {@code "effect.chill"}. The UI keys its art by it. */
    String key();

    /** The one name text uses: {@code "Chill"}. */
    String title();

    /** What it does, its numbers read from its own rules; lower-case first, no full stop. */
    TermText describe();
}
