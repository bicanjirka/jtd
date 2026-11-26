package td.tower.upgrade;

import td.term.Term;
import td.term.TermText;
import td.tower.TowerBuffState;
import td.tower.TowerStats;
import td.tower.ability.AbilityCharge;

import java.util.Optional;
import java.util.Set;

/** A mechanic a node adds to its tower, and what it adds in words. */
public interface Mechanic {

    /** A buff this mechanic's state gives the tower now, if any. */
    default Optional<TowerBuffState> condition() {
        return Optional.empty();
    }

    /** The ability this mechanic gives its tower, charging up; renewing the mechanic starts its bar over. */
    default Optional<AbilityCharge> ability() {
        return Optional.empty();
    }

    /** Every term this mechanic's tower puts on enemies or gives towers because of it. */
    default Set<Term> applies() {
        return Set.of();
    }

    /** What this mechanic adds, lower-case first, no full stop: "every 4th shell is a [Nuke Shell]". */
    TermText describe();

    /**
     * What this mechanic adds on a tower that has {@code stats} now: a figure it works out from a share of the tower's
     * hit says the points it comes to. The words without a tower by default.
     */
    default TermText describe(TowerStats stats) {
        return this.describe();
    }
}
