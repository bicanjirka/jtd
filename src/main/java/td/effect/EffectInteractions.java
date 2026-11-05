package td.effect;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The rules by which one active effect acts on another, in one place: what a pool earns while it lasts, and what an
 * active kind keeps out. A rule is data here, never a condition in {@code ActiveEffects}.
 */
final class EffectInteractions {

    /** A pool to the lasting marks it earns while it lasts. */
    private static final Map<EffectKind, List<EffectKind>> EARNS = new EnumMap<>(EffectKind.class);

    /** A kind to the kinds that keep it out: they strip nothing already there. */
    private static final Map<EffectKind, Set<EffectKind>> KEPT_OUT_BY = new EnumMap<>(EffectKind.class);

    static {
        EARNS.put(EffectKind.BURN, List.of(EffectKind.SCORCHED));
        EARNS.put(EffectKind.POISON, List.of(EffectKind.SICKENED));
        EARNS.put(EffectKind.SOULFIRE, List.of(EffectKind.SCORCHED, EffectKind.SICKENED));
        KEPT_OUT_BY.put(EffectKind.INVISIBLE, EnumSet.of(EffectKind.REVEALED));
    }

    private EffectInteractions() {
    }

    /** The lasting marks {@code pool} earns while it lasts; none for any other kind. */
    static List<EffectKind> earnedBy(EffectKind pool) {
        return EARNS.getOrDefault(pool, List.of());
    }

    /** Whether any pool in {@code active} is still earning stacks of {@code debuff}. */
    static boolean isEarnedBy(EffectKind debuff, Set<EffectKind> active) {
        return EARNS.entrySet().stream()
                .anyMatch(earns -> active.contains(earns.getKey()) && earns.getValue().contains(debuff));
    }

    /** The first of the {@code active} kinds, in kind order, that keeps {@code kind} out. */
    static Optional<EffectKind> keptOutBy(EffectKind kind, Set<EffectKind> active) {
        for (EffectKind other : EffectKind.values()) {
            if (active.contains(other) && KEPT_OUT_BY.getOrDefault(kind, Set.of()).contains(other)) {
                return Optional.of(other);
            }
        }
        return Optional.empty();
    }

    /** Whether one of the {@code active} kinds keeps {@code incoming} out. */
    static boolean blocks(Set<EffectKind> active, EffectKind incoming) {
        return KEPT_OUT_BY.getOrDefault(incoming, Set.of()).stream().anyMatch(active::contains);
    }

    /** Every kind that the {@code active} kinds currently keep out. */
    static Set<EffectKind> blockedBy(Set<EffectKind> active) {
        Set<EffectKind> blocked = EnumSet.noneOf(EffectKind.class);
        KEPT_OUT_BY.forEach((kind, blockers) -> {
            if (blockers.stream().anyMatch(active::contains)) {
                blocked.add(kind);
            }
        });
        return blocked;
    }
}
