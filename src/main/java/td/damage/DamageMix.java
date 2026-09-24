package td.damage;

/**
 * How much damage of each type has landed. Combines by adding per type; {@link #none()} is the
 * identity.
 */
public record DamageMix(long physical, long magic) {

    private static final DamageMix NONE = new DamageMix(0, 0);

    public static DamageMix none() {
        return NONE;
    }

    public static DamageMix of(Damage damage) {
        return switch (damage.type()) {
            case PHYSICAL -> new DamageMix(damage.amount(), 0);
            case MAGIC -> new DamageMix(0, damage.amount());
        };
    }

    public DamageMix plus(DamageMix other) {
        return new DamageMix(this.physical + other.physical, this.magic + other.magic);
    }

    public long total() {
        return this.physical + this.magic;
    }

    /** The type with more landed damage; physical on a tie. */
    public DamageType dominant() {
        return this.magic > this.physical ? DamageType.MAGIC : DamageType.PHYSICAL;
    }

    /**
     * How one-sided the mix is: {@code 0} for an even split or no damage at all, {@code 1} when
     * every point landed as one type.
     */
    public float dominance() {
        long total = this.total();
        if (total == 0) {
            return 0f;
        }
        return (float) Math.abs(this.physical - this.magic) / total;
    }
}
