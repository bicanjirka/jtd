package td.damage;

/**
 * The running {@link DamageMix} of every hit that landed this level. The game loop records; any
 * thread may read the published mix.
 */
public final class DamageTally {

    private volatile DamageMix mix = DamageMix.none();

    public void record(Damage landed) {
        if (landed.amount() > 0) {
            this.mix = this.mix.plus(DamageMix.of(landed));
        }
    }

    public DamageMix mix() {
        return this.mix;
    }

    public void clear() {
        this.mix = DamageMix.none();
    }
}
