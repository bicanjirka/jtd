package td.enemy;

import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.effect.ShieldGauge;
import td.stat.EnemyStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

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
 * @param blocked      kinds an active effect keeps out, in kind order
 * @param diminished   per kind whose diminishing returns run, the share the next one would last
 * @param traitLines   one row per authored trait slot
 */
public record EnemyInspection(String name, String description, Rank rank, BodyArchetype archetype, int health,
                              int maxHealth, int bounty, Map<EnemyStat, Float> stats, List<EffectState> effects,
                              List<EffectKind> blocked,
                              Map<EffectKind, Float> diminished, List<TraitLine> traitLines, Fate fate) {

    public EnemyInspection {
        stats = Collections.unmodifiableMap(new EnumMap<>(stats));
        effects = List.copyOf(effects);
        blocked = List.copyOf(blocked);
        diminished = diminished.isEmpty() ? Map.of() : Collections.unmodifiableMap(new EnumMap<>(diminished));
        traitLines = List.copyOf(traitLines);
    }

    /** {@code mob} as it is now. */
    public static EnemyInspection of(DefinedEnemyMob mob) {
        EnemyDefinition definition = mob.definition();
        Map<EnemyStat, Float> stats = new EnumMap<>(EnemyStat.class);
        for (EnemyStat stat : EnemyStat.values()) {
            stats.put(stat, mob.stats().value(stat));
        }
        List<EffectState> effects = new ArrayList<>();
        for (EffectKind kind : mob.activeEffectKinds()) {
            effects.add(EffectState.of(kind, mob.effectRemainingTicks(kind)).withStacks(mob.effectStacks(kind))
                    .withLevel(mob.effectLevel(kind))
                    .withShield(kind == EffectKind.SHIELD ? mob.shieldGauge() : ShieldGauge.none()));
        }
        List<TraitLine> traitLines = definition.traitSlots().stream().map(slot -> slot.template().describe()).toList();
        return new EnemyInspection(definition.displayName(), definition.description(), mob.getRank(), mob.archetype(),
                (mob.getHealth() + DamageUnits.PER_POINT - 1) / DamageUnits.PER_POINT, mob.getMaxHealthPoints(), mob.getBounty(), stats, effects, List.copyOf(mob.blockedEffectKinds()),
                mob.diminishedFactors(), traitLines, mob.fate());
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
    public record EffectState(EffectKind kind, OptionalInt remainingTicks, int stacks, float level, ShieldGauge shield) {

        /** An effect with no stacks, level or shield; add them with the {@code withX} copies. */
        public static EffectState of(EffectKind kind, OptionalInt remainingTicks) {
            return new EffectState(kind, remainingTicks, 0, 0f, ShieldGauge.none());
        }

        public EffectState withStacks(int stacks) {
            return new EffectState(this.kind, this.remainingTicks, stacks, this.level, this.shield);
        }

        public EffectState withLevel(float level) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, level, this.shield);
        }

        public EffectState withShield(ShieldGauge shield) {
            return new EffectState(this.kind, this.remainingTicks, this.stacks, this.level, shield);
        }
    }
}
