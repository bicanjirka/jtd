package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

import java.util.List;

/** Something that happened to an enemy carrying one of the Hexer's hexes. */
public sealed interface HexEvent {

    /**
     * A hex ran out on a living enemy.
     *
     * @param stored the damage the enemy took while it lasted, in units
     */
    record Ended(EnemyMob enemy, EffectKind kind, long stored) implements HexEvent {
    }

    /**
     * An enemy carrying the Hexer's hexes is gone: killed, or out of the fight another way.
     *
     * @param hexes every hex of this Hexer it carried
     */
    record Gone(EnemyMob enemy, List<CarriedHex> hexes) implements HexEvent {

        public Gone {
            hexes = List.copyOf(hexes);
        }
    }

    /**
     * One hex an enemy carried.
     *
     * @param castTicks how long it was cast for, before the enemy lengthened or shortened it
     */
    record CarriedHex(EffectKind kind, int castTicks) {
    }
}
