package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

/** What a payload may make the Seeker do to the enemy its missile reached. */
public interface PayloadActions {

    /** Freezes {@code target} as a missile does, for {@code strength} times as long. */
    void freeze(EnemyMob target, float strength);

    /** Adds {@code stacks} of the stacking debuff {@code kind} to {@code target}. */
    void applyStacks(EnemyMob target, EffectKind kind, int stacks);

    /** Silences {@code target} for {@code ticks}. */
    void silence(EnemyMob target, int ticks);

    /** Strips the shield and the heal {@code target} has. */
    void dispel(EnemyMob target);

    /** Reveals {@code target} to every tower and marks it for {@code ticks}. */
    void spot(EnemyMob target, int ticks);
}
