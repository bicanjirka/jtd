package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;

/** Ricochet: a crit hops on once to an enemy beside the target, picked at random. */
public final class RicochetMechanic implements SniperMechanic {

    private static final float HOP_REACH_CELLS = 3f;

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.ricochet(result.target(), HOP_REACH_CELLS);
        }
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit also hits another enemy within " + TermNumbers.cells(HOP_REACH_CELLS)
                + ", picked at random, and that hit can crit too");
    }
}
