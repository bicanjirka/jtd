package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The hexes one Hexer has on enemies now. The enemy's effect is the hex's timer; the ledger only watches for it to
 * end or for its carrier to go, and remembers how much damage the enemy had taken when it was cast and how long it was
 * cast for.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HexLedger {

    private final List<Entry> entries = new ArrayList<>();

    /** A hex of {@code kind}, cast for {@code castTicks}, just went on {@code enemy}; one it had already starts over. */
    public void record(EnemyMob enemy, EffectKind kind, int castTicks) {
        this.entries.removeIf(entry -> entry.enemy() == enemy && entry.kind() == kind);
        this.entries.add(new Entry(enemy, kind, -enemy.damageTaken(), castTicks));
    }

    /**
     * A hex of {@code kind} jumped onto {@code enemy} carrying {@code stored}: one it already had merges with it,
     * keeping both records and the longer cast.
     */
    public void recordJump(EnemyMob enemy, EffectKind kind, int castTicks, long stored) {
        long held = this.entries.stream()
                .filter(entry -> entry.enemy() == enemy && entry.kind() == kind)
                .mapToLong(Entry::stored).sum();
        int cast = this.entries.stream()
                .filter(entry -> entry.enemy() == enemy && entry.kind() == kind)
                .mapToInt(Entry::castTicks).max().orElse(castTicks);
        this.entries.removeIf(entry -> entry.enemy() == enemy && entry.kind() == kind);
        this.entries.add(new Entry(enemy, kind, held + stored - enemy.damageTaken(), Math.max(cast, castTicks)));
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * What happened to the hexes since the last call: each one that ran out on a living enemy, and each enemy that is
     * gone carrying some, once with all of them. Either way they are no longer watched.
     */
    public List<HexEvent> settle() {
        List<HexEvent> events = new ArrayList<>();
        // In the order the hexes were cast, so a run reproduces.
        Map<EnemyMob, List<HexEvent.CarriedHex>> gone = new LinkedHashMap<>();
        this.entries.removeIf(entry -> {
            if (entry.enemy().isDead()) {
                gone.computeIfAbsent(entry.enemy(), enemy -> new ArrayList<>())
                        .add(new HexEvent.CarriedHex(entry.kind(), entry.castTicks(), entry.stored()));
                return true;
            }
            if (entry.enemy().hasEffect(entry.kind())) {
                return false;
            }
            events.add(new HexEvent.Ended(entry.enemy(), entry.kind(),
                    entry.stored()));
            return true;
        });
        gone.forEach((enemy, hexes) -> events.add(new HexEvent.Gone(enemy, hexes)));
        return events;
    }

    /** {@code offset} is what the hex carried in less what the enemy had taken when it went on. */
    private record Entry(EnemyMob enemy, EffectKind kind, long offset, int castTicks) {

        /** What the enemy took under the hex, and what the hex carried in. */
        long stored() {
            return this.enemy.damageTaken() + this.offset;
        }
    }
}
