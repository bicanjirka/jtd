package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

/** What a hex may put on an enemy as it lands, credited to its Hexer, at the strength it was cast with. */
public interface HexHand {

    /** The hex {@code kind} for {@code ticks}. */
    void curse(EnemyMob target, EffectKind kind, int ticks);

    /** Blight for {@code ticks}: a slow growing by {@code slowPerTick} from {@code start}. */
    void blight(EnemyMob target, int ticks, float slowPerTick, float start);

    /** Poison for {@code ticks}, each tick {@code baseShare} of base damage, as magic. */
    void poison(EnemyMob target, float baseShare, int ticks);
}
