package td.tower.pulse;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.util.ThreadConfined;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The stacks one field last gave each enemy inside of each effect it holds at a strength. A field may take back only
 * what it gave: when its own strength falls (Toll eaten, the enemy farther from the centre), it lowers an effect it
 * holds, but never one a stronger field raised past it.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class FieldHolds {

    private final Map<EnemyMob, Map<EffectKind, Integer>> given = new IdentityHashMap<>();

    /**
     * Brings {@code enemy}'s {@code kind} toward {@code strength}: lowers it if this field holds it higher, and tells
     * how many stacks to add to raise it, {@code 0} for a refresh.
     */
    public int toward(EnemyMob enemy, EffectKind kind, int strength) {
        Map<EffectKind, Integer> mine = this.given.computeIfAbsent(enemy, key -> new EnumMap<>(EffectKind.class));
        int current = enemy.effectStacks(kind);
        Integer last = mine.get(kind);
        if (current > strength && last != null && last == current) {
            enemy.loseStacks(kind, current - strength);
            current = strength;
        }
        if (current > strength) {
            mine.remove(kind);
            return 0;
        }
        mine.put(kind, strength);
        return strength - current;
    }

    /** Forgets every enemy not in {@code inside}: what it carries away is no longer this field's to lower. */
    public void keepOnly(List<EnemyMob> inside) {
        if (!this.given.isEmpty()) {
            this.given.keySet().removeIf(enemy -> !inside.contains(enemy));
        }
    }
}
