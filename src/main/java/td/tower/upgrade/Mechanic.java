package td.tower.upgrade;

import td.term.Term;
import td.term.TermText;
import td.tower.TowerBuffState;

import java.util.Optional;
import java.util.Set;

/** A mechanic a node adds to its tower, and what it adds in words. */
public interface Mechanic {

    /** A buff this mechanic's state gives the tower now, if any. */
    default Optional<TowerBuffState> condition() {
        return Optional.empty();
    }

    /** Every term this mechanic's tower puts on enemies or gives towers because of it. */
    default Set<Term> applies() {
        return Set.of();
    }

    /** What this mechanic adds, lower-case first, no full stop: "every 4th shell is a [Nuke Shell]". */
    TermText describe();
}
