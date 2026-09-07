package td.damage;

/**
 * An amount of damage about to be dealt to an enemy. The compact constructor clamps every
 * construction path at zero, so a falloff or resistance calculation that would otherwise go
 * negative (see {@code TowerTwo}'s splash falloff) can never produce a healing hit.
 * {@link #none()} is the identity element for {@link #plus}.
 */
public record Damage(int amount) {

    private static final Damage NONE = new Damage(0);

    public Damage {
        amount = Math.max(0, amount);
    }

    public static Damage none() {
        return NONE;
    }

    public static Damage of(int amount) {
        return new Damage(amount);
    }

    public Damage plus(Damage other) {
        return new Damage(this.amount + other.amount);
    }

    public Damage scaledBy(float factor) {
        return new Damage(Math.round(this.amount * factor));
    }
}
