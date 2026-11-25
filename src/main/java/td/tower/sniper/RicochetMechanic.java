package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;

/**
 * Ricochet: a crit hops on to an enemy beside its target, picked at random, as a shot of its own; a hop that crits hops
 * on again, up to a cap, never back to an enemy the shot already struck.
 */
public final class RicochetMechanic implements SniperMechanic {

    private static final float HOP_REACH_CELLS = 3f;
    private static final int MAX_HOPS = 1;

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.ricochet(result.target(), HOP_REACH_CELLS, MAX_HOPS);
        }
    }

    @Override
    public TermText describe() {
        String hop = "a crit also hits another enemy within " + TermNumbers.cells(HOP_REACH_CELLS)
                + ", as a shot of its own at full damage that can crit too";
        return TermText.of(MAX_HOPS > 1
                ? hop + "; a hop that crits hops on, up to " + MAX_HOPS + " hops, never back to an enemy already hit"
                : hop);
    }
}
