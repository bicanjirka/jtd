package td.effect;

/**
 * What a tower has made of the burn or poison pool it feeds, carried by the pool: how much it holds,
 * how fast it marks the enemy, what freezing it does and what it costs the enemy besides. A pool two
 * towers feed holds the stronger of each, and every flag either of them set.
 *
 * @param capFactor        the most a pool holds, as a multiple of its strongest application
 * @param stackRate        how many times as fast it earns the marks a pool earns
 * @param freezeBurstShare the share of what is left of it that lands at once when a freeze puts it out
 * @param cauterizes       whether the enemy receives half the healing and shielding while it burns
 * @param heats            whether the enemy takes more damage over time from every source while it burns
 * @param marksForMagic    whether the enemy takes more magic damage from every source while it burns
 */
public record PoolTuning(float capFactor, int stackRate, float freezeBurstShare, boolean cauterizes, boolean heats,
                         boolean marksForMagic) {

    /** What every pool is, unless a tower tunes it. */
    public static PoolTuning standard() {
        return new PoolTuning(2f, 1, 0.5f, false, false, false);
    }

    public PoolTuning withCapFactor(float capFactor) {
        return new PoolTuning(capFactor, this.stackRate, this.freezeBurstShare, this.cauterizes, this.heats,
                this.marksForMagic);
    }

    public PoolTuning withStackRate(int stackRate) {
        return new PoolTuning(this.capFactor, stackRate, this.freezeBurstShare, this.cauterizes, this.heats,
                this.marksForMagic);
    }

    public PoolTuning withFreezeBurstShare(float freezeBurstShare) {
        return new PoolTuning(this.capFactor, this.stackRate, freezeBurstShare, this.cauterizes, this.heats,
                this.marksForMagic);
    }

    public PoolTuning cauterizing() {
        return new PoolTuning(this.capFactor, this.stackRate, this.freezeBurstShare, true, this.heats,
                this.marksForMagic);
    }

    public PoolTuning heating() {
        return new PoolTuning(this.capFactor, this.stackRate, this.freezeBurstShare, this.cauterizes, true,
                this.marksForMagic);
    }

    public PoolTuning marking() {
        return new PoolTuning(this.capFactor, this.stackRate, this.freezeBurstShare, this.cauterizes, this.heats,
                true);
    }

    /** The stronger of each setting of this and {@code other}. */
    PoolTuning strongerWith(PoolTuning other) {
        return new PoolTuning(Math.max(this.capFactor, other.capFactor), Math.max(this.stackRate, other.stackRate),
                Math.max(this.freezeBurstShare, other.freezeBurstShare), this.cauterizes || other.cauterizes,
                this.heats || other.heats, this.marksForMagic || other.marksForMagic);
    }
}
