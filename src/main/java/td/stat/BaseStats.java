package td.stat;

import java.util.Arrays;

/** An immutable base value per {@link EnemyStat}; unset stats keep their default. */
public final class BaseStats {

    private static final BaseStats DEFAULTS = new BaseStats(defaultValues());

    private final float[] values;

    private BaseStats(float[] values) {
        this.values = values;
    }

    public static BaseStats defaults() {
        return DEFAULTS;
    }

    private static float[] defaultValues() {
        EnemyStat[] stats = EnemyStat.values();
        float[] values = new float[stats.length];
        for (EnemyStat stat : stats) {
            values[stat.ordinal()] = stat.defaultBase();
        }
        return values;
    }

    public BaseStats with(EnemyStat stat, float value) {
        float[] copy = this.values.clone();
        copy[stat.ordinal()] = value;
        return new BaseStats(copy);
    }

    public float value(EnemyStat stat) {
        return this.values[stat.ordinal()];
    }

    @Override
    public boolean equals(Object other) {
        return other == this || (other != null && other.getClass() == BaseStats.class
                && Arrays.equals(this.values, ((BaseStats) other).values));
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
