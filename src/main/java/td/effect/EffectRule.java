package td.effect;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.Side;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * What one effect kind is: its category, how it stacks, what paces it, what each stack does, and its
 * tags. Each {@code withX}-style method is a copy.
 *
 * @param category   the group for diminishing returns and the inspector
 * @param resistedBy the resistance that shortens it, if any
 * @param stacking   how applications combine
 * @param stackCap   the most stacks it holds; 1 for a kind that does not stack
 * @param pacing     what paces its timer or its stacks' decay
 * @param perStack   what each stack does to the enemy's stats
 * @param tags       the yes/no rules that ask for it
 * @param side       whom it works for, seen from the enemy that carries it
 */
public record EffectRule(EffectCategory category, Optional<EnemyStat> resistedBy, EffectStacking stacking, int stackCap,
                         EffectPacing pacing, StatModifiers<EnemyStat> perStack, Set<EffectTag> tags, Side side) {

    public EffectRule {
        tags = tags.isEmpty() ? Set.of() : Set.copyOf(tags);
    }

    /** A kind of {@code category} that refreshes, is paced by itself, does nothing to stats and has no tags. */
    public static EffectRule of(EffectCategory category) {
        return new EffectRule(category, Optional.empty(), EffectStacking.REFRESH, 1, EffectPacing.OWN,
                StatModifiers.none(), Set.of(), Side.DEBUFF);
    }

    public EffectRule resistedBy(EnemyStat stat) {
        return new EffectRule(this.category, Optional.of(stat), this.stacking, this.stackCap, this.pacing, this.perStack,
                this.tags, this.side);
    }

    public EffectRule stackingOnASharedClock(int cap) {
        return this.stackingAs(EffectStacking.SHARED_CLOCK, cap);
    }

    public EffectRule earnedUpTo(int cap) {
        return this.stackingAs(EffectStacking.EARNED, cap);
    }

    public EffectRule asALevel() {
        return this.stackingAs(EffectStacking.LEVEL, 1);
    }

    public EffectRule asAPool() {
        return this.stackingAs(EffectStacking.POOL, 1);
    }

    public EffectRule pacedBySpirit() {
        return this.pacedAs(EffectPacing.SPIRIT);
    }

    public EffectRule pacedBySpiritUnfloored() {
        return this.pacedAs(EffectPacing.SPIRIT_UNFLOORED);
    }

    /** Each stack adds {@code amount} flat to {@code stat}. */
    public EffectRule perStack(EnemyStat stat, float amount) {
        return this.perStack(stat, StatModifier.flat(amount));
    }

    public EffectRule perStack(EnemyStat stat, StatModifier modifier) {
        return new EffectRule(this.category, this.resistedBy, this.stacking, this.stackCap, this.pacing,
                this.perStack.and(stat, modifier), this.tags, this.side);
    }

    public EffectRule tagged(EffectTag... tags) {
        Set<EffectTag> all = this.tags.isEmpty() ? EnumSet.noneOf(EffectTag.class) : EnumSet.copyOf(this.tags);
        all.addAll(Set.of(tags));
        return new EffectRule(this.category, this.resistedBy, this.stacking, this.stackCap, this.pacing, this.perStack,
                all, this.side);
    }

    /** This kind, working for the enemy that carries it. */
    public EffectRule helpsCarrier() {
        return new EffectRule(this.category, this.resistedBy, this.stacking, this.stackCap, this.pacing, this.perStack,
                this.tags, Side.BUFF);
    }

    private EffectRule stackingAs(EffectStacking stacking, int cap) {
        return new EffectRule(this.category, this.resistedBy, stacking, cap, this.pacing, this.perStack, this.tags, this.side);
    }

    private EffectRule pacedAs(EffectPacing pacing) {
        return new EffectRule(this.category, this.resistedBy, this.stacking, this.stackCap, pacing, this.perStack,
                this.tags, this.side);
    }
}
