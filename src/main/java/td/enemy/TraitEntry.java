package td.enemy;

import td.term.Term;
import td.term.TermText;

/**
 * One trait slot as the player sees it: the term its icon shows, how much of that term it resists (0 for a trait
 * that resists nothing, 1 for an immunity) and what it does.
 */
public record TraitEntry(Term term, float resisted, TermText text) {

    public static TraitEntry of(Term term, TermText text) {
        return new TraitEntry(term, 0f, text);
    }

    public boolean immune() {
        return this.resisted >= 1f;
    }
}
