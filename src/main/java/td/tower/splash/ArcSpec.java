package td.tower.splash;

/**
 * How the Stormcaller's arcs run past the blast. Each {@code withX} is a copy, and every change
 * holds whatever order the perks were bought in: jumps add up, the share and the bonuses only grow.
 *
 * @param active             whether the blast arcs at all
 * @param extraJumps         jumps on top of the base two
 * @param minimumJumps       the fewest jumps it makes, whatever else it has
 * @param share              each arc's damage, a share of the blast's
 * @param canCrit            whether an arc may crit
 * @param critBonus          extra crit chance on an arc
 * @param saturatedCritBonus extra crit chance on an arc into a fully Saturated enemy
 * @param dazeSeconds        how long an arc crit Dazes its target; {@code 0} for not at all
 * @param longDaze           whether that Daze lasts {@link #LONG_DAZE_SECONDS} instead
 * @param forks              whether the third jump forks into two arcs
 * @param returns            how many times an arc with nowhere to go returns to the primary
 * @param stoppedCritDamage  extra crit damage on an arc into a frozen or Dazed enemy
 */
public record ArcSpec(boolean active, int extraJumps, int minimumJumps, float share, boolean canCrit,
                      float critBonus, float saturatedCritBonus, float dazeSeconds, boolean longDaze, boolean forks,
                      int returns, float stoppedCritDamage) {

    public static final int BASE_JUMPS = 2;
    public static final float BASE_SHARE = 0.5f;
    public static final float LONG_DAZE_SECONDS = 1f;
    /** How far an arc jumps from an enemy, in cells, and how much further per Saturation stack it carries. */
    private static final float REACH_CELLS = 1.5f;
    private static final float REACH_CELLS_PER_STACK = 0.5f;

    public static ArcSpec none() {
        return new ArcSpec(false, 0, 0, 0f, false, 0f, 0f, 0f, false, false, 0, 0f);
    }

    /** Arc: two jumps at half the blast, as magic, that can't crit yet. */
    public static ArcSpec arcs() {
        return new ArcSpec(true, 0, 0, BASE_SHARE, false, 0f, 0f, 0f, false, false, 0, 0f);
    }

    public int jumps() {
        return Math.max(this.minimumJumps, BASE_JUMPS + this.extraJumps);
    }

    /** How long an arc crit Dazes; {@code 0} when it doesn't. */
    public float daze() {
        if (this.dazeSeconds <= 0f) {
            return 0f;
        }
        return this.longDaze ? LONG_DAZE_SECONDS : this.dazeSeconds;
    }

    /** How far, in cells before the blast radius bonus, an arc jumps from an enemy with {@code stacks} Saturation. */
    public static float reachCells(int stacks) {
        return REACH_CELLS + REACH_CELLS_PER_STACK * stacks;
    }

    public ArcSpec withMoreJumps(int jumps) {
        return new ArcSpec(this.active, this.extraJumps + jumps, this.minimumJumps, this.share, this.canCrit,
                this.critBonus, this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks, this.returns,
                this.stoppedCritDamage);
    }

    public ArcSpec withMinimumJumps(int minimumJumps) {
        return new ArcSpec(this.active, this.extraJumps, Math.max(this.minimumJumps, minimumJumps), this.share,
                this.canCrit, this.critBonus, this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks,
                this.returns, this.stoppedCritDamage);
    }

    public ArcSpec withShareAtLeast(float share) {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, Math.max(this.share, share), this.canCrit,
                this.critBonus, this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks, this.returns,
                this.stoppedCritDamage);
    }

    public ArcSpec thatCrit() {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, true, this.critBonus,
                this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks, this.returns,
                this.stoppedCritDamage);
    }

    public ArcSpec withCritBonus(float critBonus) {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit,
                this.critBonus + critBonus, this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks,
                this.returns, this.stoppedCritDamage);
    }

    public ArcSpec withSaturatedCritBonus(float bonus) {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit, this.critBonus,
                this.saturatedCritBonus + bonus, this.dazeSeconds, this.longDaze, this.forks, this.returns,
                this.stoppedCritDamage);
    }

    public ArcSpec withDaze(float seconds) {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit, this.critBonus,
                this.saturatedCritBonus, Math.max(this.dazeSeconds, seconds), this.longDaze, this.forks, this.returns,
                this.stoppedCritDamage);
    }

    /** An arc crit's Daze, whenever it has one, lasts {@link #LONG_DAZE_SECONDS}. */
    public ArcSpec withLongDaze() {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit, this.critBonus,
                this.saturatedCritBonus, this.dazeSeconds, true, this.forks, this.returns, this.stoppedCritDamage);
    }

    public ArcSpec thatForks() {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit, this.critBonus,
                this.saturatedCritBonus, this.dazeSeconds, this.longDaze, true, this.returns, this.stoppedCritDamage);
    }

    public ArcSpec withReturns(int returns) {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit, this.critBonus,
                this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks, Math.max(this.returns, returns),
                this.stoppedCritDamage);
    }

    public ArcSpec withStoppedCritDamage(float bonus) {
        return new ArcSpec(this.active, this.extraJumps, this.minimumJumps, this.share, this.canCrit, this.critBonus,
                this.saturatedCritBonus, this.dazeSeconds, this.longDaze, this.forks, this.returns,
                this.stoppedCritDamage + bonus);
    }
}
