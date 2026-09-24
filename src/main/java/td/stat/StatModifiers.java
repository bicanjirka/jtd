package td.stat;

import java.util.EnumMap;
import java.util.Map;

/**
 * An immutable bundle of {@link StatModifier}s, at most one per stat. {@link #plus} combines two
 * bundles stat by stat; {@link #none()} is the identity. A constant bundle feeds a
 * {@link StatAccumulator} without allocating.
 */
public final class StatModifiers {

    private static final StatModifiers NONE = new StatModifiers(new EnumMap<>(EnemyStat.class));

    private final EnemyStat[] stats;
    private final StatModifier[] modifiers;

    private StatModifiers(Map<EnemyStat, StatModifier> byStat) {
        this.stats = byStat.keySet().toArray(new EnemyStat[0]);
        this.modifiers = byStat.values().toArray(new StatModifier[0]);
    }

    public static StatModifiers none() {
        return NONE;
    }

    public static StatModifiers of(EnemyStat stat, StatModifier modifier) {
        return NONE.and(stat, modifier);
    }

    /** This bundle with {@code modifier} combined into {@code stat}. */
    public StatModifiers and(EnemyStat stat, StatModifier modifier) {
        Map<EnemyStat, StatModifier> byStat = this.toMap();
        byStat.merge(stat, modifier, StatModifier::plus);
        return new StatModifiers(byStat);
    }

    public StatModifiers plus(StatModifiers other) {
        Map<EnemyStat, StatModifier> byStat = this.toMap();
        for (int i = 0; i < other.stats.length; i++) {
            byStat.merge(other.stats[i], other.modifiers[i], StatModifier::plus);
        }
        return new StatModifiers(byStat);
    }

    /** {@link StatModifier#none()} for a stat this bundle leaves alone. */
    public StatModifier get(EnemyStat stat) {
        for (int i = 0; i < this.stats.length; i++) {
            if (this.stats[i] == stat) {
                return this.modifiers[i];
            }
        }
        return StatModifier.none();
    }

    public void applyTo(StatAccumulator accumulator) {
        for (int i = 0; i < this.stats.length; i++) {
            accumulator.add(this.stats[i], this.modifiers[i]);
        }
    }

    private Map<EnemyStat, StatModifier> toMap() {
        Map<EnemyStat, StatModifier> byStat = new EnumMap<>(EnemyStat.class);
        for (int i = 0; i < this.stats.length; i++) {
            byStat.put(this.stats[i], this.modifiers[i]);
        }
        return byStat;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other != null && other.getClass() == StatModifiers.class
                && this.toMap().equals(((StatModifiers) other).toMap()));
    }

    @Override
    public int hashCode() {
        return this.toMap().hashCode();
    }

    @Override
    public String toString() {
        return "StatModifiers" + this.toMap();
    }
}
