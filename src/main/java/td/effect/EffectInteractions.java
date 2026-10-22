package td.effect;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The rules by which one active effect acts on another, in one place. While a kind is active it
 * blocks each kind it lists, and applying it removes those kinds if they are already there. A
 * kind can also consume another without blocking it: applying it removes the other, once. The other
 * tables bend fire and ice, say what a kind needs or counts as, and what a pool earns. A rule is data
 * here, never a condition in {@code ActiveEffects}.
 */
final class EffectInteractions {

    /** Blocked kind to the kinds that block it: a frozen enemy cannot burn. */
    private static final Map<EffectKind, Set<EffectKind>> BLOCKED_BY = new EnumMap<>(EffectKind.class);

    /** Consumed kind to the kinds that consume it: freezing a chilled enemy removes the chill, and shrouding a revealed one hides it again. */
    private static final Map<EffectKind, Set<EffectKind>> CONSUMED_BY = new EnumMap<>(EffectKind.class);

    /** Blocked kind to the kinds that only keep it out: they never remove one that is already there. */
    private static final Map<EffectKind, Set<EffectKind>> HELD_OFF_BY = new EnumMap<>(EffectKind.class);

    /** A kind to the kind it only counts while: brittleness needs a freeze. */
    private static final Map<EffectKind, EffectKind> REQUIRES = new EnumMap<>(EffectKind.class);
    /** A kind to the kind it counts as too: a revealed enemy is exposed. */
    private static final Map<EffectKind, EffectKind> IMPLIES = new EnumMap<>(EffectKind.class);
    /** A pool to the lasting marks it earns while it lasts. */
    private static final Map<EffectKind, List<EffectKind>> EARNS = new EnumMap<>(EffectKind.class);
    /** A kind to the chill it counts as when a freeze lands: a held Undertow is a quarter. */
    private static final Map<EffectKind, Float> CHILL_EQUIVALENT = new EnumMap<>(EffectKind.class);
    /** A kind to how many times a freeze's extra time from chill it buys: twice under Rime. */
    private static final Map<EffectKind, Float> FREEZE_CHILL_WEIGHT = new EnumMap<>(EffectKind.class);
    /** A kind to the ticks a freeze it suffers lasts longer: a second when tarred. */
    private static final Map<EffectKind, Integer> FREEZE_EXTRA_TICKS = new EnumMap<>(EffectKind.class);
    /** A kind to how much stronger a fire it catches starts: double when tarred. */
    private static final Map<EffectKind, Float> FIRE_START_FACTOR = new EnumMap<>(EffectKind.class);
    /** The kinds under which a freeze lands all of the burn it puts out. */
    private static final Set<EffectKind> FULL_FREEZE_BURST = EnumSet.noneOf(EffectKind.class);
    /** A kind to the share of a chill the enemy still takes: a quarter under Ash. */
    private static final Map<EffectKind, Float> CHILL_TAKEN = new EnumMap<>(EffectKind.class);
    /** A kind to how many times as much a pool holds, and as fast it earns its marks: twice under Ash. */
    private static final Map<EffectKind, Integer> POOL_FACTOR = new EnumMap<>(EffectKind.class);

    /** The least share of the burn a freeze puts out that lands at once. */
    static final float FREEZE_BURST_FLOOR = 0.5f;

    static {
        REQUIRES.put(EffectKind.BRITTLE, EffectKind.FREEZE);
        IMPLIES.put(EffectKind.REVEALED, EffectKind.EXPOSED);
        EARNS.put(EffectKind.BURN, List.of(EffectKind.SCORCHED));
        EARNS.put(EffectKind.POISON, List.of(EffectKind.SICKENED));
        EARNS.put(EffectKind.SOULFIRE, List.of(EffectKind.SCORCHED, EffectKind.SICKENED));
        CHILL_EQUIVALENT.put(EffectKind.UNDERTOW, 0.25f);
        FREEZE_CHILL_WEIGHT.put(EffectKind.RIME, 2f);
        FREEZE_EXTRA_TICKS.put(EffectKind.TARRED, 20);
        FIRE_START_FACTOR.put(EffectKind.TARRED, 2f);
        FULL_FREEZE_BURST.add(EffectKind.RIME);
        CHILL_TAKEN.put(EffectKind.ASH, 0.25f);
        POOL_FACTOR.put(EffectKind.ASH, 2);
        for (EffectKind slow : List.of(EffectKind.CHILL, EffectKind.TARRED, EffectKind.DRAGGED, EffectKind.ANCHORED)) {
            BLOCKED_BY.put(slow, EnumSet.of(EffectKind.HASTENED));
        }
        BLOCKED_BY.put(EffectKind.BURN, EnumSet.of(EffectKind.FREEZE));
        BLOCKED_BY.put(EffectKind.SOULFIRE, EnumSet.of(EffectKind.FREEZE));
        CONSUMED_BY.put(EffectKind.CHILL, EnumSet.of(EffectKind.FREEZE));
        CONSUMED_BY.put(EffectKind.REVEALED, EnumSet.of(EffectKind.INVISIBLE));
        BLOCKED_BY.put(EffectKind.FREEZE, EnumSet.of(EffectKind.ASH));
        CONSUMED_BY.put(EffectKind.RIME, EnumSet.of(EffectKind.ASH));
        CONSUMED_BY.put(EffectKind.ASH, EnumSet.of(EffectKind.RIME));
        BLOCKED_BY.put(EffectKind.INVISIBLE, EnumSet.of(EffectKind.INVERSION, EffectKind.UNVEILED));
        HELD_OFF_BY.put(EffectKind.HEAL, EnumSet.of(EffectKind.DEAD_ZONE, EffectKind.FALLOUT));
        HELD_OFF_BY.put(EffectKind.SHIELD, EnumSet.of(EffectKind.DEAD_ZONE));
    }

    private EffectInteractions() {
    }

    /** Whether {@code kind} counts on an enemy with the {@code active} kinds: what it needs is there. */
    static boolean contributes(EffectKind kind, Set<EffectKind> active) {
        EffectKind required = REQUIRES.get(kind);
        return required == null || active.contains(required);
    }

    /** Whether {@code kind} is active, or another active kind counts as it. */
    static boolean isActiveOrImplied(EffectKind kind, Set<EffectKind> active) {
        if (active.contains(kind)) {
            return true;
        }
        for (Map.Entry<EffectKind, EffectKind> implied : IMPLIES.entrySet()) {
            if (implied.getValue() == kind && active.contains(implied.getKey())) {
                return true;
            }
        }
        return false;
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

    /** What a freeze's length is multiplied by on an enemy chilled by {@code chillLevel} with the {@code active} kinds. */
    static float freezeLengthFactor(Set<EffectKind> active, float chillLevel) {
        float chill = chillLevel;
        for (Map.Entry<EffectKind, Float> equivalent : CHILL_EQUIVALENT.entrySet()) {
            if (active.contains(equivalent.getKey())) {
                chill += equivalent.getValue();
            }
        }
        float weight = 1f;
        for (Map.Entry<EffectKind, Float> rule : FREEZE_CHILL_WEIGHT.entrySet()) {
            if (active.contains(rule.getKey())) {
                weight = Math.max(weight, rule.getValue());
            }
        }
        return 1f + weight * chill;
    }

    /** The ticks a freeze lasts longer on an enemy with the {@code active} kinds. */
    static int freezeExtraTicks(Set<EffectKind> active) {
        int extra = 0;
        for (Map.Entry<EffectKind, Integer> rule : FREEZE_EXTRA_TICKS.entrySet()) {
            if (active.contains(rule.getKey())) {
                extra += rule.getValue();
            }
        }
        return extra;
    }

    /** How much stronger a fire that starts fresh begins on an enemy with the {@code active} kinds. */
    static float fireStartFactor(Set<EffectKind> active) {
        float factor = 1f;
        for (Map.Entry<EffectKind, Float> rule : FIRE_START_FACTOR.entrySet()) {
            if (active.contains(rule.getKey())) {
                factor *= rule.getValue();
            }
        }
        return factor;
    }

    /** The share of a burn a freeze lands at once: all of it under a full-burst kind, else at least half of {@code tuned}. */
    static float freezeBurstShare(Set<EffectKind> active, float tuned) {
        for (EffectKind kind : FULL_FREEZE_BURST) {
            if (active.contains(kind)) {
                return 1f;
            }
        }
        return Math.max(FREEZE_BURST_FLOOR, tuned);
    }

    /** The share of a chill an enemy with the {@code active} kinds still takes. */
    static float chillTaken(Set<EffectKind> active) {
        float taken = 1f;
        for (Map.Entry<EffectKind, Float> rule : CHILL_TAKEN.entrySet()) {
            if (active.contains(rule.getKey())) {
                taken *= rule.getValue();
            }
        }
        return taken;
    }

    /** How many times as much a pool holds, and as fast it earns, on an enemy with the {@code active} kinds. */
    static int poolFactor(Set<EffectKind> active) {
        int factor = 1;
        for (Map.Entry<EffectKind, Integer> rule : POOL_FACTOR.entrySet()) {
            if (active.contains(rule.getKey())) {
                factor *= rule.getValue();
            }
        }
        return factor;
    }

    /** The first of the {@code active} kinds, in kind order, that keeps {@code kind} out or holds it off. */
    static Optional<EffectKind> keptOutBy(EffectKind kind, Set<EffectKind> active) {
        for (EffectKind other : EffectKind.values()) {
            if (active.contains(other) && (BLOCKED_BY.getOrDefault(kind, Set.of()).contains(other)
                    || HELD_OFF_BY.getOrDefault(kind, Set.of()).contains(other))) {
                return Optional.of(other);
            }
        }
        return Optional.empty();
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
