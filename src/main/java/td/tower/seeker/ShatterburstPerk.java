package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

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

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SILENCED, EffectKind.UNRAVELED);
    }

    @Override
    public TermText describe() {
        return TermText.of("a missile that hits an enemy with ").plus(EffectKind.FREEZE)
                .plus(" bursts for " + TermNumbers.percent(SHARE)
                        + " magic damage around it; every enemy the burst hits gains ")
                .plus(EffectKind.SILENCED).plus(" for " + TermNumbers.seconds(SILENCE_TICKS / (float) TickRate.TICKS_PER_SECOND) + " and a stack of ")
                .plus(EffectKind.UNRAVELED);
    }
}
