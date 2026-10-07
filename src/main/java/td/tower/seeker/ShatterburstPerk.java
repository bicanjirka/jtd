package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.TickRate;

/**
 * Shatterburst: a missile that hits a frozen enemy bursts around it; every enemy the burst hits is
 * Silenced and gains an Unraveled.
 */
public final class ShatterburstPerk implements SeekerPerk {

    private static final float SHARE = 0.35f;
    private static final float RADIUS_CELLS = 1.5f;
    private static final int SILENCE_TICKS = Math.round(2f * TickRate.TICKS_PER_SECOND);

    @Override
    public void react(Impact impact, SeekerActions actions) {
        if (!impact.wasFrozen()) {
            return;
        }
        for (EnemyMob enemy : actions.burst(impact.target(), SHARE, RADIUS_CELLS)) {
            actions.silence(enemy, SILENCE_TICKS);
            actions.applyStacks(enemy, EffectKind.UNRAVELED, 1);
        }
    }
}
