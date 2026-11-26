package td.stat;

import java.util.Arrays;

/** Every stat of one key type resolved once, immutable; the snapshot form of a stat sheet. */
public final class ResolvedStats<S extends Enum<S> & StatKey> {

    private final float[] values;

    private ResolvedStats(float[] values) {
        this.values = values;
    }

    public static <S extends Enum<S> & StatKey> ResolvedStats<S> resolve(BaseStats<S> base, StatModifiers<S> modifiers,
                                                                       Class<S> type) {
        S[] keys = type.getEnumConstants();
        float[] values = new float[keys.length];
        for (S key : keys) {
            values[key.ordinal()] = key.clamp(modifiers.get(key).applyTo(base.value(key)));
        }
        return new ResolvedStats<>(values);
    }

    public float value(S stat) {
        return this.values[stat.ordinal()];
    }

    /** Whether every stat resolved to the same value as in {@code other}; compares without allocating. */
    public boolean sameValuesAs(ResolvedStats<S> other) {
        return Arrays.equals(this.values, other.values);
    }
}
