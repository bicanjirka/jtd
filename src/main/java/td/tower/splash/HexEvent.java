package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

/** Something that happened to an enemy carrying one of the Hexer's hexes. */
public sealed interface HexEvent {

    /**
     * A hex ran out on a living enemy.
     *
     * @param stored the damage the enemy took while it lasted, in units
     */
    record Ended(EnemyMob enemy, EffectKind kind, long stored) implements HexEvent {
    }
}
