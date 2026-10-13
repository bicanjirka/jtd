package td.tower.sniper;

import td.tower.upgrade.PerkText;

/** Ricochet: a crit bounces on to the enemies beside the target. */
public final class RicochetPerk implements SniperPerk {

    private static final float BOUNCE_RADIUS_CELLS = 1.5f;
    private static final int BOUNCES = 3;
    private static final float SHARE = 0.6f;

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.ricochet(result.target(), BOUNCES, SHARE, BOUNCE_RADIUS_CELLS);
        }
    }

    @Override
    public String describe() {
        return "a crit bounces to the nearest enemy within " + PerkText.cells(BOUNCE_RADIUS_CELLS) + " cells for "
                + PerkText.percent(SHARE) + ", up to " + BOUNCES + " bounces, each can crit";
    }
}
