package td.effect;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The rules by which one active effect acts on another, in one table. While a kind is active it
 * blocks each kind it lists, and applying it removes those kinds if they are already there. A
 * kind can also consume another without blocking it: applying it removes the other, once. A rule is
 * data here, never a condition in {@code ActiveEffects}.
 */
final class EffectInteractions {

    /** Blocked kind to the kinds that block it: a frozen enemy cannot burn. */
    private static final Map<EffectKind, Set<EffectKind>> BLOCKED_BY = new EnumMap<>(EffectKind.class);

    /** Consumed kind to the kinds that consume it: freezing a chilled enemy removes the chill, and shrouding a revealed one hides it again. */
    private static final Map<EffectKind, Set<EffectKind>> CONSUMED_BY = new EnumMap<>(EffectKind.class);

    /** Blocked kind to the kinds that only keep it out: they never remove one that is already there. */
    private static final Map<EffectKind, Set<EffectKind>> HELD_OFF_BY = new EnumMap<>(EffectKind.class);

    static {
        BLOCKED_BY.put(EffectKind.BURN, EnumSet.of(EffectKind.FREEZE));
        CONSUMED_BY.put(EffectKind.CHILL, EnumSet.of(EffectKind.FREEZE));
        CONSUMED_BY.put(EffectKind.REVEALED, EnumSet.of(EffectKind.INVISIBLE));
        BLOCKED_BY.put(EffectKind.FREEZE, EnumSet.of(EffectKind.ASH));
        CONSUMED_BY.put(EffectKind.RIME, EnumSet.of(EffectKind.ASH));
        CONSUMED_BY.put(EffectKind.ASH, EnumSet.of(EffectKind.RIME));
        BLOCKED_BY.put(EffectKind.INVISIBLE, EnumSet.of(EffectKind.INVERSION));
        HELD_OFF_BY.put(EffectKind.HEAL, EnumSet.of(EffectKind.DEAD_ZONE));
        HELD_OFF_BY.put(EffectKind.SHIELD, EnumSet.of(EffectKind.DEAD_ZONE));
    }

    private EffectInteractions() {
    }

    /** Whether one of the {@code active} kinds keeps {@code incoming} out. */
    static boolean blocks(Set<EffectKind> active, EffectKind incoming) {
        return BLOCKED_BY.getOrDefault(incoming, Set.of()).stream().anyMatch(active::contains)
                || HELD_OFF_BY.getOrDefault(incoming, Set.of()).stream().anyMatch(active::contains);
    }

    /** The kinds that applying {@code incoming} removes, whether it then keeps them out or not. */
    static Set<EffectKind> removedBy(EffectKind incoming) {
        Set<EffectKind> removed = EnumSet.noneOf(EffectKind.class);
        BLOCKED_BY.forEach((blocked, blockers) -> {
            if (blockers.contains(incoming)) {
                removed.add(blocked);
            }
        });
        CONSUMED_BY.forEach((consumed, consumers) -> {
            if (consumers.contains(incoming)) {
                removed.add(consumed);
            }
        });
        return removed;
    }

    /** Every kind that the {@code active} kinds currently keep out. */
    static Set<EffectKind> blockedBy(Set<EffectKind> active) {
        Set<EffectKind> blocked = EnumSet.noneOf(EffectKind.class);
        BLOCKED_BY.forEach((kind, blockers) -> {
            if (blockers.stream().anyMatch(active::contains)) {
                blocked.add(kind);
            }
        });
        HELD_OFF_BY.forEach((kind, blockers) -> {
            if (blockers.stream().anyMatch(active::contains)) {
                blocked.add(kind);
            }
        });
        return blocked;
    }
}
