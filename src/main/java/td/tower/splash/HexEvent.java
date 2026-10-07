package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

import java.util.List;
import java.util.Optional;

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
     * An enemy carrying the Hexer's hexes died.
     *
     * @param hexes every hex of this Hexer it carried
     */
    record Died(EnemyMob enemy, List<CarriedHex> hexes) implements HexEvent {

        public Died {
            hexes = List.copyOf(hexes);
        }

        /** The hex of {@code kind} it carried; empty when it carried none. */
        public Optional<CarriedHex> carried(EffectKind kind) {
            return this.hexes.stream().filter(hex -> hex.kind() == kind).findFirst();
        }
    }

    /**
     * One hex an enemy carried.
     *
     * @param generation how many times it has jumped from enemy to enemy
     */
    record CarriedHex(EffectKind kind, int generation) {
    }
}
