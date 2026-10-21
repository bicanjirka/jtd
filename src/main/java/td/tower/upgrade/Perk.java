package td.tower.upgrade;

import td.tower.TowerBuffState;

import java.util.Optional;

/** A mechanic a node adds to its tower, and what it adds in words. */
public interface Perk {

    /** A buff this perk's state gives the tower now, if any. */
    default Optional<TowerBuffState> condition() {
        return Optional.empty();
    }

    /** What this perk adds, lower-case first, no full stop: "every 4th shell is a nuke: x4 damage over x1.5 the blast". */
    String describe();
}
