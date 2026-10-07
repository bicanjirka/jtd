package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

/** What a hex may make the Hexer do as it casts. */
public interface HexActions {

    /** Puts the hex {@code kind} on {@code target} for {@code ticks}, credited to the Hexer. */
    void curse(EnemyMob target, EffectKind kind, int ticks);

    /** Poisons {@code target} for {@code ticks}, each tick {@code weaponShare} of the Hexer's damage, as magic. */
    void poison(EnemyMob target, float weaponShare, int ticks);

    /** Chills {@code target} by {@code amount}, fading over {@code ticks}. */
    void chill(EnemyMob target, float amount, int ticks);
}
