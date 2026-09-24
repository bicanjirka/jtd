package td.damage;

/**
 * An amount of damage, its {@link DamageType}, and whether it is critical. Clamped at zero, so no
 * calculation can produce a healing hit.
 * <p>
 * A zero amount is the identity for {@link #plus} whatever either side's type; adding two non-zero
 * damages of different types throws.
 * <p>
 * {@link #critical()} survives {@link #scaledBy} and {@link #cappedAt}, so a resisted critical hit
 * still counts as one.
 */
public record Damage(int amount, DamageType type, boolean critical) {

    private static final Damage NONE = new Damage(0, DamageType.PHYSICAL);

    public Damage {
        amount = Math.max(0, amount);
    }

    /** A non-critical hit. */
    public Damage(int amount, DamageType type) {
        this(amount, type, false);
    }

    public static Damage none() {
        return NONE;
    }

    public static Damage physical(int amount) {
        return new Damage(amount, DamageType.PHYSICAL);
    }

    public static Damage magic(int amount) {
        return new Damage(amount, DamageType.MAGIC);
    }

    /**
     * @throws IllegalArgumentException if both sides are non-zero and their types differ
     */
    public Damage plus(Damage other) {
        if (this.amount == 0) {
            return other;
        }
        if (other.amount == 0) {
            return this;
        }
        if (this.type != other.type) {
            throw new IllegalArgumentException("Cannot combine " + this.type + " and " + other.type + " damage");
        }
        return new Damage(this.amount + other.amount, this.type, this.critical || other.critical);
    }

    public Damage scaledBy(float factor) {
        return new Damage(Math.round(this.amount * factor), this.type, this.critical);
    }

    /** Type and critical flag are preserved. */
    public Damage cappedAt(int max) {
        return new Damage(Math.min(this.amount, max), this.type, this.critical);
    }

    /** Marks the hit critical and multiplies it; a hit is rolled critical at most once. */
    public Damage asCritical(float multiplier) {
        return new Damage(Math.round(this.amount * multiplier), this.type, true);
    }
}
