package td.damage;

/**
 * An amount of damage, tagged with the {@link DamageType} it was dealt as, about to be dealt
 * to an enemy. The compact constructor clamps every construction path at zero, so a falloff
 * or resistance calculation that would otherwise go negative (see {@code SplashTower}'s splash
 * falloff) can never produce a healing hit. {@link #none()} is the identity element for
 * {@link #plus}.
 * <p>
 * A zero-amount {@code Damage} is the identity for {@link #plus} <em>regardless of its own or
 * the other side's type</em> - {@code Damage.magic(0).plus(Damage.physical(5))} is
 * {@code Damage.physical(5)}, not a type mismatch. Combining two non-zero damages of
 * different types has no sensible meaning and throws.
 */
public record Damage(int amount, DamageType type) {

    private static final Damage NONE = new Damage(0, DamageType.PHYSICAL);

    public Damage {
        amount = Math.max(0, amount);
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
        return new Damage(this.amount + other.amount, this.type);
    }

    public Damage scaledBy(float factor) {
        return new Damage(Math.round(this.amount * factor), this.type);
    }

    /** This damage's amount, capped at {@code max} - the type is preserved either way. */
    public Damage cappedAt(int max) {
        return new Damage(Math.min(this.amount, max), this.type);
    }
}
