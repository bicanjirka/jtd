package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;

/** Ricochet: a crit hops on once to the enemy beside the target. */
public final class RicochetMechanic implements SniperMechanic {

    private static final float HOP_REACH_CELLS = 1.5f;

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.ricochet(result.target(), HOP_REACH_CELLS);
        }
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit also hits the nearest other enemy within " + TermNumbers.cells(HOP_REACH_CELLS)
                + "; that hit can't crit");
    }
}
