package td.stat;

import td.term.Term;

import java.util.Optional;

/**
 * A number the player reads: its short name for rows, its full name ({@link #title()}) for hovers and mentions,
 * what it does ({@link #describe()}), how its value reads, which way is better and where its row sits.
 */
public interface StatTerm extends Term {

    /** The row's name: the full name when that is short. */
    String shortName();

    /** How a stored value becomes the number and unit the player reads. */
    StatUnit unit();

    StatDirection direction();

    StatGroup group();

    /** What {@code value} means, short enough to follow it on a row; empty when only the hover says it. */
    default Optional<String> inline(float value) {
        return Optional.empty();
    }
}
