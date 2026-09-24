package td.stat;

/**
 * A change to one stat. A stat resolves as {@code (base + flat) * (1 + percentAdd) * multiply},
 * replaced by {@code setTo} when one is present.
 * <p>
 * {@link #plus} adds flats and percentages, multiplies multipliers and keeps the lower set value,
 * so a freeze (speed 0) or a reveal (stealth 0) wins over anything that sets higher.
 * {@link #none()} is the identity; "no set value" is {@link Float#POSITIVE_INFINITY}, the identity
 * of that minimum.
 */
public record StatModifier(float flat, float percentAdd, float multiply, float setTo) {

    private static final StatModifier NONE = new StatModifier(0f, 0f, 1f, Float.POSITIVE_INFINITY);

    public static StatModifier none() {
        return NONE;
    }

    public static StatModifier flat(float amount) {
        return new StatModifier(amount, 0f, 1f, Float.POSITIVE_INFINITY);
    }

    public static StatModifier percent(float fraction) {
        return new StatModifier(0f, fraction, 1f, Float.POSITIVE_INFINITY);
    }

    public static StatModifier times(float factor) {
        return new StatModifier(0f, 0f, factor, Float.POSITIVE_INFINITY);
    }

    public static StatModifier setTo(float value) {
        return new StatModifier(0f, 0f, 1f, value);
    }

    public StatModifier plus(StatModifier other) {
        return new StatModifier(this.flat + other.flat, this.percentAdd + other.percentAdd,
                this.multiply * other.multiply, Math.min(this.setTo, other.setTo));
    }

    public boolean hasSetValue() {
        return this.setTo != Float.POSITIVE_INFINITY;
    }

    /** {@code base} with this modifier applied, unclamped. */
    public float applyTo(float base) {
        if (this.hasSetValue()) {
            return this.setTo;
        }
        return (base + this.flat) * (1f + this.percentAdd) * this.multiply;
    }
}
