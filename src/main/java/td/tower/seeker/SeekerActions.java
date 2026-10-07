package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

import java.util.List;

/** What a perk may make the Seeker do once a missile has landed. */
public interface SeekerActions {

    /**
     * Hits every other enemy within {@code radiusCells} of {@code center} for {@code share} of the
     * Seeker's damage, as magic.
     *
     * @return the enemies it hit
     */
    List<EnemyMob> burst(EnemyMob center, float share, float radiusCells);

    /** Silences {@code target} for {@code ticks}. */
    void silence(EnemyMob target, int ticks);

    /** Adds {@code stacks} of the stacking debuff {@code kind} to {@code target}. */
    void applyStacks(EnemyMob target, EffectKind kind, int stacks);

    /** Launches a missile from the tower at once, at whom it would pick now; its freeze rearms nothing. */
    void rearm();
}
