package td.tower.seeker;

import td.effect.StackingDebuff;
import td.enemy.EnemyMob;

/** What a payload may make the Seeker do to the enemy its missile reached. */
public interface PayloadActions {

    /** Freezes {@code target} as a missile does, for {@code strength} times as long. */
    void freeze(EnemyMob target, float strength);

    /** Adds {@code stacks} of {@code debuff} to {@code target}. */
    void applyStacks(EnemyMob target, StackingDebuff debuff, int stacks);

    /** Silences {@code target} for {@code ticks}. */
    void silence(EnemyMob target, int ticks);

    /** Strips the shield and the heal {@code target} has. */
    void dispel(EnemyMob target);

    /** Reveals {@code target} to every tower and marks it for {@code ticks}. */
    void spot(EnemyMob target, int ticks);
}
