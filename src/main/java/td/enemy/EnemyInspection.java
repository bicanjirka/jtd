package td.enemy;

import td.damage.DamageUnits;
import td.effect.ActiveEffects;
import td.effect.EffectKind;
import td.effect.ShieldGauge;
import td.stat.EnemyStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;

/**
 * An immutable snapshot of one enemy for display: identity, health, every resolved stat, active
 * effects, the kinds they keep out, diminishing returns, one line per trait, and what became
 * of it. Taken on the thread
 * that owns the mob and read anywhere.
 *
 * @param health       current health in points
 * @param maxHealth    full health in points
 * @param stats        every stat's resolved value
 * @param effects      active effects in kind order
 * @param dodges       how many hits it has dodged
 * @param baseStats    every stat before traits and effects
 * @param immunities   what it cannot take now, and what keeps it out
 * @param traitEntries every trait as a term, with its sentence
 * @param abilityEntries every ability as a term, with its sentence
 * @param breakdowns   what each shown stat is made of
 * @param marks        memory towers keep about it
 */
public record EnemyInspection(String name, String description, Rank rank, BodyArchetype archetype, int health,
                              int maxHealth, int bounty, Map<EnemyStat, Float> stats, List<EffectState> effects,
                              Fate fate,
                              int dodges, Map<EnemyStat, Float> baseStats, List<Immunity> immunities,
                              List<TraitEntry> traitEntries, List<AbilityEntry> abilityEntries,
                              Map<EnemyStat, StatBreakdown> breakdowns, List<EnemyMark> marks) {

    public EnemyInspection {
        stats = Collections.unmodifiableMap(new EnumMap<>(stats));
        effects = List.copyOf(effects);
        baseStats = Collections.unmodifiableMap(new EnumMap<>(baseStats));
        immunities = List.copyOf(immunities);
        traitEntries = List.copyOf(traitEntries);
        abilityEntries = List.copyOf(abilityEntries);
        breakdowns = breakdowns.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(breakdowns));
        marks = List.copyOf(marks);
    }

    /** {@code mob} as it is now, read from its parts; {@link DefinedEnemyMob#inspect()} is the way in. */
    static EnemyInspection of(DefinedEnemyMob mob) {
        ActiveEffects live = mob.liveEffects();
        EnemyDefinition definition = mob.definition();
        Map<EnemyStat, Float> stats = new EnumMap<>(EnemyStat.class);
        for (EnemyStat stat : EnemyStat.values()) {
            stats.put(stat, mob.stats().value(stat));
        }
        Map<EnemyStat, Float> baseStats = new EnumMap<>(EnemyStat.class);
        Map<EnemyStat, StatBreakdown> breakdowns = new EnumMap<>(EnemyStat.class);
        for (EnemyStat stat : EnemyStat.values()) {
            baseStats.put(stat, mob.baseStat(stat));
            if (StatRows.shows(stat, mob.baseStat(stat), stats.get(stat))) {
                breakdowns.put(stat, mob.breakdown(stat));
            }
        }
        Map<EffectKind, Float> diminished = mob.diminishedFactors();
        Set<EffectKind> active = live.activeKinds();
        List<EffectState> effects = new ArrayList<>();
        for (EffectKind kind : mob.effectKindsByGain()) {
            EffectState state = active.contains(kind) ? activeState(live, kind) : EffectState.faded(kind);
            Float nextShare = diminished.get(kind);
            effects.add(nextShare == null ? state : state.withNextShare(nextShare));
        }
        List<Immunity> immunities = live.blockedKinds().stream()
                .map(kind -> new Immunity(kind, live.keptOutBy(kind))).toList();
        List<TraitEntry> traitEntries = new ArrayList<>(
                definition.traitSlots().stream().map(slot -> slot.template().entry()).toList());
        float cellSize = mob.cellSize();
        definition.disruption().ifPresent(aura -> traitEntries
                .add(TraitEntry.of(TraitTerm.JAM, AbilityText.jam(aura, cellSize))));
        EnemyCatalog catalog = mob.enemyCatalog();
        AbilityText.Words words = new AbilityText.Words(cellSize,
                id -> catalog.contains(id) ? catalog.get(id).displayName() : id);
        List<AbilityEntry> abilityEntries = definition.abilities().stream()
                .map(ability -> new AbilityEntry(ability.term(), AbilityText.of(ability, words))).toList();
        return new EnemyInspection(definition.displayName(), definition.description(), mob.getRank(), mob.archetype(),
                (mob.getHealth() + DamageUnits.PER_POINT - 1) / DamageUnits.PER_POINT, mob.getMaxHealthPoints(), mob.getBounty(), stats, effects,
                mob.fate(), mob.dodges(), baseStats, immunities, traitEntries,
                abilityEntries, breakdowns, List.of());
    }

    private static EffectState activeState(ActiveEffects live, EffectKind kind) {
        return EffectState.of(kind, live.remainingTicks(kind)).withStacks(live.stacks(kind))
                .withLevel(kind == EffectKind.CHILL ? live.chillLevel() : 0f)
                .withShield(kind == EffectKind.SHIELD ? live.shieldGauge() : ShieldGauge.none())
                .withClockTicks(live.clockTicks(kind))
                .withFullness(live.fullness(kind));
    }

    /** A copy carrying {@code marks}: memory towers keep about this enemy, filled in by the frame build. */
    public EnemyInspection withMarks(List<EnemyMark> marks) {
        return new EnemyInspection(this.name, this.description, this.rank, this.archetype, this.health,
                this.maxHealth, this.bounty, this.stats, this.effects,
                this.fate, this.dodges, this.baseStats, this.immunities, this.traitEntries,
                this.abilityEntries, this.breakdowns, marks);
    }

    /** An effect kind this enemy cannot take now, and what keeps it out. */
    public record Immunity(EffectKind kind, Optional<EffectKind> keptOutBy) {
    }

    public float stat(EnemyStat stat) {
        return this.stats.get(stat);
    }

    /** What happened to the enemy; a snapshot of a gone enemy keeps its last values. */
    public enum Fate {
        ALIVE,
        KILLED,
        LEAKED,
        HATCHED
    }

    /**
     * One active effect.
     *
     * @param remainingTicks ticks left; empty for a fuel pool, which decays rather than counting down
     * @param stacks         a vulnerability's, burn's or poison's stack count; {@code 0} for other kinds
     * @param level          how much speed a chill takes away, from {@code 0} to {@code 1}; {@code 0} for other kinds
     * @param shield         a shield's pool, in points; {@link ShieldGauge#none()} for other kinds
     */
    public record EffectState(EffectKind kind, OptionalInt remainingTicks, int stacks, float level, ShieldGauge shield,
                              OptionalInt clockTicks, OptionalDouble fullness, boolean faded,
                              OptionalDouble nextShare) {

        /** An effect with no stacks, level or shield; add them with the {@code withX} copies. */
        public static EffectState of(EffectKind kind, OptionalInt remainingTicks) {
            return new EffectState(kind, remainingTicks, 0, 0f, ShieldGauge.none(), OptionalInt.empty(),
                    OptionalDouble.empty(), false, OptionalDouble.empty());
        }

        /** A kind that ended but whose diminishing returns still run. */
        public static EffectState faded(EffectKind kind) {
            return new EffectState(kind, OptionalInt.empty(), 0, 0f, ShieldGauge.none(), OptionalInt.empty(),
                    OptionalDouble.empty(), true, OptionalDouble.empty());
        }

        public EffectState withStacks(int stacks) {
            return new EffectState(this.kind, this.remainingTicks, stacks, this.level, this.shield, this.clockTicks,
                    this.fullness, this.faded, this.nextShare);
        }

        public EffectState withLevel(float level) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, level, this.shield, this.clockTicks,
                    this.fullness, this.faded, this.nextShare);
        }

        public EffectState withShield(ShieldGauge shield) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level, shield, this.clockTicks,
                    this.fullness, this.faded, this.nextShare);
        }

        public EffectState withClockTicks(OptionalInt clockTicks) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level, this.shield, clockTicks,
                    this.fullness, this.faded, this.nextShare);
        }

        public EffectState withFullness(OptionalDouble fullness) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level, this.shield,
                    this.clockTicks, fullness, this.faded, this.nextShare);
        }

        public EffectState withNextShare(float nextShare) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level, this.shield,
                    this.clockTicks, this.fullness, this.faded, OptionalDouble.of(nextShare));
        }
    }
}
