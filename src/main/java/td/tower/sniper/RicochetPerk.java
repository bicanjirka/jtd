package td.tower.sniper;

import td.tower.targeting.MostNeighboursSelector;

/** Ricochet: a crit bounces on to the enemies beside the target. Aims where a bounce finds the most. */
public final class RicochetPerk implements SniperPerk {

    private static final float BOUNCE_RADIUS_CELLS = 1.5f;
    private static final int BOUNCES = 3;
    private static final float SHARE = 0.6f;

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.aimingAt(new SniperAim(new MostNeighboursSelector(BOUNCE_RADIUS_CELLS * spec.view().cellSize()),
                "most neighbours"));
    }

    @Override
    public void react(ShotResult result, ShotActions actions) {
        if (result.critical()) {
            actions.ricochet(result.target(), BOUNCES, SHARE, BOUNCE_RADIUS_CELLS);
        }
    }
}
