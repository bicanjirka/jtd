package td.enemy;

import td.term.Term;
import td.term.TermText;

/** One trait slot as the player sees it: the term its icon shows, whether it is immune to that term, and what it does. */
public record TraitEntry(Term term, boolean immune, TermText text) {

    public static TraitEntry of(Term term, TermText text) {
        return new TraitEntry(term, false, text);
    }

    public static TraitEntry immunity(Term term, TermText text) {
        return new TraitEntry(term, true, text);
    }
}
