package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.List;

/**
 * The hexes one Hexer has on enemies now. The enemy's effect is the hex's timer; the ledger only
 * watches for it to end, and remembers how much damage the enemy had taken when it was cast.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HexLedger {

    private final List<Entry> entries = new ArrayList<>();

    /** A hex of {@code kind} just went on {@code enemy}. */
    public void record(EnemyMob enemy, EffectKind kind) {
        this.entries.add(new Entry(enemy, kind, enemy.damageTaken()));
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * What happened to the hexes since the last call: each one that ran out on a living enemy. A
     * hex on an enemy that died is dropped. Either way it is no longer watched.
     */
    public List<HexEvent> settle() {
        List<HexEvent> events = new ArrayList<>();
        this.entries.removeIf(entry -> {
            if (entry.enemy().isDead()) {
                return true;
            }
            if (entry.enemy().hasEffect(entry.kind())) {
                return false;
            }
            events.add(new HexEvent.Ended(entry.enemy(), entry.kind(),
                    entry.enemy().damageTaken() - entry.damageTakenAtCast()));
            return true;
        });
        return events;
    }

    private record Entry(EnemyMob enemy, EffectKind kind, long damageTakenAtCast) {
    }
}
