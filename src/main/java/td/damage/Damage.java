package td.damage;

/**
 * An amount of damage, tagged with the {@link DamageType} it was dealt as and whether it landed
 * as a critical hit, about to be dealt to an enemy. The compact constructor clamps every
 * construction path at zero, so a falloff or resistance calculation that would otherwise go
 * negative (see {@code SplashTower}'s splash falloff) can never produce a healing hit.
 * {@link #none()} is the identity element for {@link #plus}.
 * <p>
 * A zero-amount {@code Damage} is the identity for {@link #plus} <em>regardless of its own or
 * the other side's type</em> - {@code Damage.magic(0).plus(Damage.physical(5))} is
 * {@code Damage.physical(5)}, not a type mismatch. Combining two non-zero damages of
 * different types has no sensible meaning and throws.
 * <p>
 * {@link #critical()} is set by {@link #asCritical()} - what {@code AbstractTower.dealDamage}
 * calls once its crit-chance roll succeeds, never a factory or a combinator. It is preserved
 * through {@link #scaledBy}/{@link #cappedAt} (a resisted or capped critical hit is still the
 * critical hit a mob "survived", which matters for an ability triggered by surviving one), and
 * stripped back off by {@link #stripCritical()}, which a critical-hit-immune trait uses instead
 * of reducing the amount by some independent fraction of its own - see
 * {@code CriticalImmunityTrait}.
 */
public record Damage(int amount, DamageType type, boolean critical) {

    /**
     * The bonus a critical hit deals, applied once by {@link #asCritical()} and undone once by
     * {@link #stripCritical()}. One project-wide constant rather than a per-tower value - see
     * {@code docs/features/FEATURE-critical-damage.md}'s Product review notes for why - the same
     * discipline {@code TickRate} already applies to the tick rate: it lives once, here, and
     * nothing else restates it.
     */
    public static final float CRITICAL_MULTIPLIER = 1.5f;

    private static final Damage NONE = new Damage(0, DamageType.PHYSICAL);

    public Damage {
        amount = Math.max(0, amount);
    }

    /**
     * Equivalent to {@code Damage(amount, type, false)} - every pre-existing two-argument
     * construction path (this record's original shape, before critical hits existed) stays
     * non-critical without needing to change.
     */
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

    /**
     * This damage's amount, capped at {@code max} - the type and critical flag are preserved either way.
     */
    public Damage cappedAt(int max) {
        return new Damage(Math.min(this.amount, max), this.type, this.critical);
    }

    /**
     * Marks this damage critical and applies {@link #CRITICAL_MULTIPLIER} - what
     * {@code AbstractTower.dealDamage} calls once its crit-chance roll succeeds. A hit is rolled
     * critical at most once, before an enemy ever sees it - this is never called on a
     * {@code Damage} that is already critical.
     */
    public Damage asCritical() {
        return new Damage(Math.round(this.amount * CRITICAL_MULTIPLIER), this.type, true);
    }

    /**
     * Undoes {@link #asCritical()}'s bonus and clears the flag - a no-op on a {@code Damage}
     * that isn't critical. Dividing back out by the one shared {@link #CRITICAL_MULTIPLIER}
     * is exact regardless of which tower's roll produced the bonus, since every tower rolls
     * against the same constant - see this type's own doc comment.
     */
    public Damage stripCritical() {
        return this.critical ? new Damage(Math.round(this.amount / CRITICAL_MULTIPLIER), this.type, false) : this;
    }
}
