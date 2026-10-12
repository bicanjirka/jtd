package td.stat;

import java.util.Arrays;

/** An immutable base value per stat; unset stats keep their default. */
public final class BaseStats<S extends Enum<S> & StatKey> {

    private final float[] values;
    private final boolean[] set;

    private BaseStats(float[] values, boolean[] set) {
        this.values = values;
        this.set = set;
    }

    public static <S extends Enum<S> & StatKey> BaseStats<S> defaults(Class<S> type) {
        S[] stats = type.getEnumConstants();
        float[] values = new float[stats.length];
        for (S stat : stats) {
            values[stat.ordinal()] = stat.defaultBase();
        }
        return new BaseStats<>(values, new boolean[stats.length]);
    }

    public BaseStats<S> with(S stat, float value) {
        float[] values = this.values.clone();
        boolean[] set = this.set.clone();
        values[stat.ordinal()] = value;
        set[stat.ordinal()] = true;
        return new BaseStats<>(values, set);
    }

    public float value(S stat) {
        return this.values[stat.ordinal()];
    }

    /** Whether content authored this stat, even at its default. */
    public boolean isSet(S stat) {
        return this.set[stat.ordinal()];
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other != null && other.getClass() == BaseStats.class
                && Arrays.equals(this.values, ((BaseStats<?>) other).values)
                && Arrays.equals(this.set, ((BaseStats<?>) other).set));
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.values);
    }

    @Override
    public String toString() {
        return "BaseStats" + Arrays.toString(this.values);
    }
}
