package td.stat;

/**
 * A change to one stat. A stat resolves as {@code (base + flat) * (1 + percentAdd) * multiply},
 * held to at most {@code base * capShareOfBase}, or replaced by {@code setTo} when one is present.
 * <p>
 * {@link #plus} adds flats and percentages, multiplies multipliers and keeps the lower set value
 * and the lower cap, so a freeze (speed 0) or a reveal (stealth 0) wins over anything that sets
 * higher. {@link #none()} is the identity; "no set value" and "no cap" are
 * {@link Float#POSITIVE_INFINITY}, the identity of that minimum.
 */
public record StatModifier(float flat, float percentAdd, float multiply, float setTo, float capShareOfBase) {

    private static final float NO_VALUE = Float.POSITIVE_INFINITY;
    private static final StatModifier NONE = new StatModifier(0f, 0f, 1f, NO_VALUE, NO_VALUE);

    public static StatModifier none() {
        return NONE;
    }

    public static StatModifier flat(float amount) {
        return new StatModifier(amount, 0f, 1f, NO_VALUE, NO_VALUE);
    }

    public static StatModifier percent(float fraction) {
        return new StatModifier(0f, fraction, 1f, NO_VALUE, NO_VALUE);
    }

    public static StatModifier times(float factor) {
        return new StatModifier(0f, 0f, factor, NO_VALUE, NO_VALUE);
    }

    public static StatModifier setTo(float value) {
        return new StatModifier(0f, 0f, 1f, value, NO_VALUE);
    }

    /** Holds the stat to at most {@code share} of its base value, whatever else raised it. */
    public static StatModifier capAtShareOfBase(float share) {
        return new StatModifier(0f, 0f, 1f, NO_VALUE, share);
    }

    public StatModifier plus(StatModifier other) {
        return new StatModifier(this.flat + other.flat, this.percentAdd + other.percentAdd,
                this.multiply * other.multiply, Math.min(this.setTo, other.setTo),
                Math.min(this.capShareOfBase, other.capShareOfBase));
    }

    public boolean hasSetValue() {
        return this.setTo != NO_VALUE;
    }

    public boolean hasCap() {
        return this.capShareOfBase != NO_VALUE;
    }

    /** {@code base} with this modifier applied, unclamped. */
    public float applyTo(float base) {
        if (this.hasSetValue()) {
            return this.setTo;
        }
        float value = (base + this.flat) * (1f + this.percentAdd) * this.multiply;
        return this.hasCap() ? Math.min(value, base * this.capShareOfBase) : value;
    }
}
