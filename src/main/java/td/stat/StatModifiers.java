package td.stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * An immutable bundle of {@link StatModifier}s, at most one per stat. {@link #plus} combines two
 * bundles stat by stat; {@link #none()} is the identity. A constant bundle feeds a
 * {@link StatAccumulator} without allocating.
 */
public final class StatModifiers<S extends Enum<S> & StatKey> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final StatModifiers NONE = new StatModifiers(List.of(), new StatModifier[0]);

    private final List<S> stats;
    private final StatModifier[] modifiers;

    private StatModifiers(List<S> stats, StatModifier[] modifiers) {
        this.stats = stats;
        this.modifiers = modifiers;
    }

    @SuppressWarnings("unchecked")
    public static <S extends Enum<S> & StatKey> StatModifiers<S> none() {
        return NONE;
    }

    public static <S extends Enum<S> & StatKey> StatModifiers<S> of(S stat, StatModifier modifier) {
        return StatModifiers.<S>none().and(stat, modifier);
    }

    private static <S extends Enum<S> & StatKey> StatModifiers<S> from(Map<S, StatModifier> byStat) {
        if (byStat.isEmpty()) {
            return none();
        }
        return new StatModifiers<>(Collections.unmodifiableList(new ArrayList<>(byStat.keySet())),
                byStat.values().toArray(new StatModifier[0]));
    }

    /** This bundle with {@code modifier} combined into {@code stat}. */
    public StatModifiers<S> and(S stat, StatModifier modifier) {
        Map<S, StatModifier> byStat = this.toMap();
        byStat.merge(stat, modifier, StatModifier::plus);
        return from(byStat);
    }

    public StatModifiers<S> plus(StatModifiers<S> other) {
        if (other.isNone()) {
            return this;
        }
        if (this.isNone()) {
            return other;
        }
        Map<S, StatModifier> byStat = this.toMap();
        for (int i = 0; i < other.modifiers.length; i++) {
            byStat.merge(other.stats.get(i), other.modifiers[i], StatModifier::plus);
        }
        return from(byStat);
    }

    /** The stats this bundle changes, in ascending ordinal order. */
    public List<S> stats() {
        return this.stats;
    }

    public boolean isNone() {
        return this.modifiers.length == 0;
    }

    /** {@link StatModifier#none()} for a stat this bundle leaves alone. */
    public StatModifier get(S stat) {
        for (int i = 0; i < this.modifiers.length; i++) {
            if (this.stats.get(i) == stat) {
                return this.modifiers[i];
            }
        }
        return StatModifier.none();
    }

    public void applyTo(StatAccumulator<S> accumulator) {
        for (int i = 0; i < this.modifiers.length; i++) {
            accumulator.add(this.stats.get(i), this.modifiers[i]);
        }
    }

    private Map<S, StatModifier> toMap() {
        Map<S, StatModifier> byStat = new TreeMap<>();
        for (int i = 0; i < this.modifiers.length; i++) {
            byStat.put(this.stats.get(i), this.modifiers[i]);
        }
        return byStat;
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other != null && other.getClass() == StatModifiers.class
                && this.toMap().equals(((StatModifiers<?>) other).toMap()));
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
