package td.tower;

import td.term.Term;
import td.term.TermText;

import java.util.List;

/**
 * A term a tower can put on enemies or give towers, with one line per source on this tower saying how.
 *
 * @param sources what applies it, in the order they were found
 */
public record AppliedEffect(Term term, List<Source> sources) {

    public AppliedEffect {
        sources = List.copyOf(sources);
    }

    /** What applies it: the tower itself or one of its nodes, by name, and how. */
    public record Source(String name, TermText how) {
    }
}
