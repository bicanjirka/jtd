package td.ui.render;

import td.term.Term;

import java.util.Optional;

/**
 * What a tooltip is about, the same from one frame to the next: "effect:effect.chill", "stat:ARMOR".
 *
 * @param term the key of the term it shows, if it shows one: what a chain compares
 */
public record TooltipKey(String id, Optional<String> term) {

    /** A tooltip about no term: a stat, a perk line, an upgrade button. */
    public static TooltipKey of(String kind, String name) {
        return new TooltipKey(kind + ":" + name, Optional.empty());
    }

    /** A tooltip about {@code term}: "effect:effect.chill". */
    public static TooltipKey ofTerm(String kind, Term term) {
        return new TooltipKey(kind + ":" + term.key(), Optional.of(term.key()));
    }

    /** One of several {@code kind} tooltips about {@code term}, told apart by {@code index}: "trait:0:effect.chill". */
    public static TooltipKey ofTerm(String kind, int index, Term term) {
        return new TooltipKey(kind + ":" + index + ":" + term.key(), Optional.of(term.key()));
    }

    /** Whether this and {@code other} show the same thing: the same term, or the same id when neither has one. */
    public boolean sameSubject(TooltipKey other) {
        return this.term.isPresent() ? this.term.equals(other.term) : this.id.equals(other.id);
    }
}
