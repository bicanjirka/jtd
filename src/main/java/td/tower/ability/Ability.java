package td.tower.ability;

import td.term.Term;

/**
 * An ability a node gives its tower: what the player calls it, the currency it charges with and what one cast costs.
 * What a cast does is the tower's, through the mechanic that brought it.
 *
 * @param term     its name, picture and meaning
 * @param currency the tower's currency, named and pictured as the player reads it
 * @param cost     what one cast takes, in whole units
 */
public record Ability(Term term, Term currency, int cost) {

    public Ability {
        if (cost < 1) {
            throw new IllegalArgumentException("An ability costs at least one unit: " + cost);
        }
    }

    public static Ability of(Term term, Term currency, int cost) {
        return new Ability(term, currency, cost);
    }
}
