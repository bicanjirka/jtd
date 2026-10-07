package td.tower.splash;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The hexes one Hexer has on enemies now. The enemy's effect is the hex's timer; the ledger only
 * watches for it to end or for its carrier to die, and remembers how much damage the enemy had
 * taken when it was cast and how often it has jumped.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class HexLedger {

    private final List<Entry> entries = new ArrayList<>();

    /** A freshly cast hex of {@code kind} just went on {@code enemy}. */
    public void record(EnemyMob enemy, EffectKind kind) {
        this.record(enemy, kind, 0);
    }

    /** A hex of {@code kind} that has jumped {@code generation} times just went on {@code enemy}. */
    public void record(EnemyMob enemy, EffectKind kind, int generation) {
        this.entries.add(new Entry(enemy, kind, enemy.damageTaken(), generation));
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** The living enemies carrying a hex of {@code kind}, in the order they were cursed. */
    public List<EnemyMob> carriers(EffectKind kind) {
        return this.entries.stream()
                .filter(entry -> entry.kind() == kind && !entry.enemy().isDead())
                .map(Entry::enemy)
                .toList();
    }

    /** The living enemies carrying any hex, each once, in the order they were first cursed. */
    public List<EnemyMob> hexed() {
        return this.entries.stream()
                .map(Entry::enemy)
                .filter(enemy -> !enemy.isDead())
                .distinct()
                .toList();
    }

    /**
     * Starts the watched hex of {@code kind} on {@code enemy} over: what it stored so far is counted
     * out, and the next count starts now.
     *
     * @return the damage the enemy took since the hex was cast, in units; {@code 0} if it carries none
     */
    public long restart(EnemyMob enemy, EffectKind kind) {
        for (int i = 0; i < this.entries.size(); i++) {
            Entry entry = this.entries.get(i);
            if (entry.enemy() == enemy && entry.kind() == kind) {
                this.entries.set(i, new Entry(enemy, kind, enemy.damageTaken(), entry.generation()));
                return enemy.damageTaken() - entry.damageTakenAtCast();
            }
        }
        return 0L;
    }

    /**
     * What happened to the hexes since the last call: each one that ran out on a living enemy, and
     * each enemy that died carrying some, once with all of them. Either way they are no longer
     * watched.
     */
    public List<HexEvent> settle() {
        List<HexEvent> events = new ArrayList<>();
        // In the order the hexes were cast, so a run reproduces.
        Map<EnemyMob, List<HexEvent.CarriedHex>> dead = new LinkedHashMap<>();
        this.entries.removeIf(entry -> {
            if (entry.enemy().isDead()) {
                dead.computeIfAbsent(entry.enemy(), enemy -> new ArrayList<>())
                        .add(new HexEvent.CarriedHex(entry.kind(), entry.generation()));
                return true;
            }
            if (entry.enemy().hasEffect(entry.kind())) {
                return false;
            }
            events.add(new HexEvent.Ended(entry.enemy(), entry.kind(),
                    entry.enemy().damageTaken() - entry.damageTakenAtCast()));
            return true;
        });
        dead.forEach((enemy, hexes) -> events.add(new HexEvent.Died(enemy, hexes)));
        return events;
    }

    private record Entry(EnemyMob enemy, EffectKind kind, long damageTakenAtCast, int generation) {
    }
}
