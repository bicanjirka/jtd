package td.tower.splash;

/**
 * How the Hexer's hexes bite. Each {@code withX} is a copy, and every change holds whatever order the mechanics were
 * bought in.
 *
 * @param doomShare     the share of the damage taken under Doom it pays out
 * @param cloud         whether Blight also curses the enemies around its target
 * @param agony         whether a hexed enemy is Vulnerable while its hex lasts
 * @param contagion     whether a hex jumps on from an enemy that dies with it
 * @param choking       whether every hex also Silences
 * @param cursesPerCast how many enemies one cast curses
 */
public record HexSpec(float doomShare, boolean cloud, boolean agony, boolean contagion, boolean choking,
                      int cursesPerCast) {

    public static final float DOOM_SHARE = 0.3f;
    /** How far around its target Blight Cloud curses, in cells. */
    public static final float CLOUD_CELLS = 1f;
    /** How far short of its enemy a Blight curse forks, with Blight Cloud, in cells. */
    public static final float CLOUD_FORK_CELLS = 2 * CLOUD_CELLS;
    /** How many enemies besides its target Blight Cloud curses: the ones that would leave soonest. */
    public static final int CLOUD_TARGETS = 3;
    /** How far a hex jumps from an enemy that died with it, in cells. */
    public static final float SPREAD_CELLS = 1.5f;

    /** The hexes as their nodes give them. */
    public static HexSpec bare() {
        return new HexSpec(DOOM_SHARE, false, false, false, false, 1);
    }

    public HexSpec withDoomShareAtLeast(float share) {
        return new HexSpec(Math.max(this.doomShare, share), this.cloud, this.agony, this.contagion, this.choking,
                this.cursesPerCast);
    }

    public HexSpec clouded() {
        return new HexSpec(this.doomShare, true, this.agony, this.contagion, this.choking, this.cursesPerCast);
    }

    public HexSpec agonizing() {
        return new HexSpec(this.doomShare, this.cloud, true, this.contagion, this.choking, this.cursesPerCast);
    }

    public HexSpec contagious() {
        return new HexSpec(this.doomShare, this.cloud, this.agony, true, this.choking, this.cursesPerCast);
    }

    public HexSpec chokingHexes() {
        return new HexSpec(this.doomShare, this.cloud, this.agony, this.contagion, true, this.cursesPerCast);
    }

    public HexSpec withCursesPerCastAtLeast(int curses) {
        return new HexSpec(this.doomShare, this.cloud, this.agony, this.contagion, this.choking,
                Math.max(this.cursesPerCast, curses));
    }
}
