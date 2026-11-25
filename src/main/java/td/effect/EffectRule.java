package td.effect;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.Side;

import java.util.EnumSet;
import java.util.Set;

/**
 * What one effect kind is: its category, how it stacks, what each stack does, and its
 * tags. Each {@code withX}-style method is a copy.
 *
 * @param category   the group for the inspector
 * @param stacking   how applications combine
 * @param stackCap   the most stacks it holds, the one cap nothing raises; 1 for a kind that does not stack, 0 for
 *                   shares that carry no stacks (a heal, a shield)
 * @param perStack   what each stack does to the enemy's stats
 * @param slowPerStack the share of speed each stack takes away; {@code 0} for a kind whose strength comes with
 *                   each application, or that doesn't slow
 * @param tags       the yes/no rules that ask for it
 * @param side       whom it works for, seen from the enemy that carries it
 */
public record EffectRule(EffectCategory category, EffectStacking stacking, int stackCap,
                         StatModifiers<EnemyStat> perStack, float slowPerStack, Set<EffectTag> tags, Side side) {

    public EffectRule {
        tags = tags.isEmpty() ? Set.of() : Set.copyOf(tags);
    }

    /** A kind of {@code category} that refreshes, does nothing to stats and has no tags. */
    public static EffectRule of(EffectCategory category) {
        return new EffectRule(category, EffectStacking.TIMED, 1,
                StatModifiers.none(), 0f, Set.of(), Side.DEBUFF);
    }

    /** A shared count of up to {@code cap} stacks, held while anything holds it and lost a stack at a time after. */
    public EffectRule countingUpTo(int cap) {
        return this.stackingAs(EffectStacking.COUNT, cap);
    }

    /** One share per source, each worth its own stacks, summed up to {@code cap}. */
    public EffectRule inSharesUpTo(int cap) {
        return this.stackingAs(EffectStacking.SHARES, cap);
    }

    /** One share per source, carrying its own amount rather than stacks: a heal, a shield. */
    public EffectRule inShares() {
        return this.stackingAs(EffectStacking.SHARES, 0);
    }

    public EffectRule asAPool() {
        return this.stackingAs(EffectStacking.POOL, 1);
    }

    /** A dose whose damage halves as it runs, applications adding their rates. */
    public EffectRule asADose() {
        return this.stackingAs(EffectStacking.DOSE, 1);
    }

    /** Each stack adds {@code amount} flat to {@code stat}. */
    public EffectRule perStack(EnemyStat stat, float amount) {
        return this.perStack(stat, StatModifier.flat(amount));
    }

    public EffectRule perStack(EnemyStat stat, StatModifier modifier) {
        return new EffectRule(this.category, this.stacking, this.stackCap,
                this.perStack.and(stat, modifier), this.slowPerStack, this.tags, this.side);
    }

    /** Each stack, or the kind if it has none, takes {@code share} of speed away. */
    public EffectRule slowingBy(float share) {
        return new EffectRule(this.category, this.stacking, this.stackCap, this.perStack, share,
                this.tags, this.side);
    }

    public EffectRule tagged(EffectTag... tags) {
        Set<EffectTag> all = this.tags.isEmpty() ? EnumSet.noneOf(EffectTag.class) : EnumSet.copyOf(this.tags);
        all.addAll(Set.of(tags));
        return new EffectRule(this.category, this.stacking, this.stackCap, this.perStack,
                this.slowPerStack, all, this.side);
    }

    /** This kind, working for the enemy that carries it. */
    public EffectRule helpsCarrier() {
        return new EffectRule(this.category, this.stacking, this.stackCap, this.perStack,
                this.slowPerStack, this.tags, Side.BUFF);
    }

    private EffectRule stackingAs(EffectStacking stacking, int cap) {
        return new EffectRule(this.category, stacking, cap, this.perStack, this.slowPerStack,
                this.tags, this.side);
    }
}
