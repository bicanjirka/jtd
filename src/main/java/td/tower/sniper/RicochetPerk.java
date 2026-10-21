package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;

/** Ricochet: a crit or a kill bounces on to the enemies beside the target. */
public final class RicochetPerk implements SniperPerk {

    private static final float BOUNCE_RADIUS_CELLS = 1.5f;
    private static final int BOUNCES = 3;
    private static final float SHARE = 0.6f;

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical() || result.killed()) {
            actions.ricochet(result.target(), BOUNCES, SHARE, BOUNCE_RADIUS_CELLS);
        }
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit or a kill bounces to the nearest enemy within " + TermNumbers.cells(BOUNCE_RADIUS_CELLS)
                + " for " + TermNumbers.percent(SHARE) + " of the damage, up to " + BOUNCES
                + " bounces; each bounce can crit");
    }
}
