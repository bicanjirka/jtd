package td.enemy;

import td.damage.DamageType;
import td.damage.DamageUnits;
import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.effect.HealShare;
import td.effect.ShieldShare;
import td.stat.EnemyStat;
import td.stat.StatBreakdown;
import td.util.TickRate;

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
 * effects, the kinds they keep out, what the next freeze lasts, one line per trait, and what became
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
 * @param livesOnLeak  the lives it takes if it leaks; {@link EnemyDefinition#ALL_LIVES} for every one left
 */
public record EnemyInspection(String name, String description, Rank rank, BodyArchetype archetype, int health,
                              int maxHealth, int bounty, Map<EnemyStat, Float> stats, List<EffectState> effects,
                              Fate fate,
                              int dodges, Map<EnemyStat, Float> baseStats, List<Immunity> immunities,
                              List<TraitEntry> traitEntries, List<AbilityEntry> abilityEntries,
                              Map<EnemyStat, StatBreakdown> breakdowns, List<EnemyMark> marks, int livesOnLeak) {

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
        Set<EffectKind> active = live.activeKinds();
        List<EffectState> effects = new ArrayList<>();
        for (EffectKind kind : mob.effectKindsByGain()) {
            EffectState state = active.contains(kind) ? activeState(live, kind) : EffectState.faded(kind);
            OptionalDouble nextShare = kind == EffectKind.FREEZE ? mob.nextFreezeShare() : OptionalDouble.empty();
            effects.add(nextShare.isPresent() ? state.withNextShare((float) nextShare.getAsDouble()) : state);
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
        return new EnemyInspection(definition.title(), definition.description(), mob.getRank(), mob.archetype(),
                (mob.getHealth() + DamageUnits.PER_POINT - 1) / DamageUnits.PER_POINT, mob.getMaxHealthPoints(), mob.getBounty(), stats, effects,
                mob.fate(), mob.dodges(), baseStats, immunities, traitEntries,
                abilityEntries, breakdowns, List.of(), definition.livesOnLeak());
    }

    private static EffectState activeState(ActiveEffects live, EffectKind kind) {
        return EffectState.of(kind, live.remainingTicks(kind)).withStacks(live.stacks(kind))
                .withLevel(live.slowNow(kind))
                .withClockTicks(live.clockTicks(kind))
                .withFullness(live.fullness(kind))
                .withShares(live.find(kind).map(EnemyInspection::sharesOf).orElse(List.of()));
    }

    private static List<ShareState> sharesOf(Effect effect) {
        List<ShareState> shares = new ArrayList<>();
        effect.healShares().forEach(heal -> shares.add(ShareState.of(heal)));
        effect.shieldShares().forEach(shield -> shares.add(ShareState.of(shield)));
        return shares;
    }

    /** A copy carrying {@code marks}: memory towers keep about this enemy, filled in by the frame build. */
    public EnemyInspection withMarks(List<EnemyMark> marks) {
        return new EnemyInspection(this.name, this.description, this.rank, this.archetype, this.health,
                this.maxHealth, this.bounty, this.stats, this.effects,
                this.fate, this.dodges, this.baseStats, this.immunities, this.traitEntries,
                this.abilityEntries, this.breakdowns, marks, this.livesOnLeak);
    }

    /** An effect kind this enemy cannot take now, and what keeps it out. */
    public record Immunity(EffectKind kind, Optional<EffectKind> keptOutBy) {
    }

    /** Whether a leak ends the level, whatever lives are left. */
    public boolean takesAllLives() {
        return this.livesOnLeak >= EnemyDefinition.ALL_LIVES;
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
     * @param shares         each source's heal or shield; empty for other kinds
     */
    public record EffectState(EffectKind kind, OptionalInt remainingTicks, int stacks, float level,
                              OptionalInt clockTicks, OptionalDouble fullness, boolean faded,
                              OptionalDouble nextShare, List<ShareState> shares) {

        public EffectState {
            shares = List.copyOf(shares);
        }

        /** An effect with no stacks, level or shares; add them with the {@code withX} copies. */
        public static EffectState of(EffectKind kind, OptionalInt remainingTicks) {
            return new EffectState(kind, remainingTicks, 0, 0f, OptionalInt.empty(),
                    OptionalDouble.empty(), false, OptionalDouble.empty(), List.of());
        }

        /** A freeze that ended while its meter still runs. */
        public static EffectState faded(EffectKind kind) {
            return new EffectState(kind, OptionalInt.empty(), 0, 0f, OptionalInt.empty(),
                    OptionalDouble.empty(), true, OptionalDouble.empty(), List.of());
        }

        public EffectState withStacks(int stacks) {
            return new EffectState(this.kind, this.remainingTicks, stacks, this.level, this.clockTicks,
                    this.fullness, this.faded, this.nextShare, this.shares);
        }

        public EffectState withLevel(float level) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, level, this.clockTicks,
                    this.fullness, this.faded, this.nextShare, this.shares);
        }

        public EffectState withClockTicks(OptionalInt clockTicks) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level, clockTicks,
                    this.fullness, this.faded, this.nextShare, this.shares);
        }

        public EffectState withFullness(OptionalDouble fullness) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level,
                    this.clockTicks, fullness, this.faded, this.nextShare, this.shares);
        }

        public EffectState withNextShare(float nextShare) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level,
                    this.clockTicks, this.fullness, this.faded, OptionalDouble.of(nextShare), this.shares);
        }

        public EffectState withShares(List<ShareState> shares) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level,
                    this.clockTicks, this.fullness, this.faded, this.nextShare, shares);
        }
    }

    /**
     * One source's heal or shield.
     *
     * @param amount         a heal's regeneration in points a second, or what a shield has left in points
     * @param max            the pool a shield was cast with; a heal's own amount
     * @param restrictedTo   the one damage type a shield takes; empty for every type, and for a heal
     * @param remainingTicks ticks left; empty for a shield that lasts until broken
     */
    public record ShareState(float amount, float max, Optional<DamageType> restrictedTo, OptionalInt remainingTicks) {

        static ShareState of(HealShare heal) {
            float perSecond = DamageUnits.inPoints(heal.perTick()) * TickRate.TICKS_PER_SECOND;
            return new ShareState(perSecond, perSecond, Optional.empty(), OptionalInt.of(heal.remainingTicks()));
        }

        static ShareState of(ShieldShare shield) {
            return new ShareState(DamageUnits.inPoints(shield.remaining()), DamageUnits.inPoints(shield.max()),
                    shield.restrictedTo(), shield.remainingTicks() == Effect.LASTING_TICKS ? OptionalInt.empty()
                            : OptionalInt.of(shield.remainingTicks()));
        }
    }
}
