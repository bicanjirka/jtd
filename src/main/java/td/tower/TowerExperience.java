package td.tower;

/**
 * One tower's experience: the XP it earned from the bounty of every enemy it reached, and when it
 * last ranked up. XP only climbs. The game loop earns; any thread reads the published snapshot.
 */
public final class TowerExperience {

    private volatile Snapshot snapshot = new Snapshot(0, -1);

    public int xp() {
        return this.snapshot.xp();
    }

    public TowerRank rank() {
        return TowerRank.of(this.snapshot.xp());
    }

    /** Ticks since the last rank-up, or {@code -1} for a tower still a Recruit. */
    public int ticksSinceRankUp(int gameTime) {
        int rankedUpAt = this.snapshot.rankedUpAt();
        return rankedUpAt < 0 ? -1 : Math.max(0, gameTime - rankedUpAt);
    }

    /** Adds {@code amount} XP during tick {@code tick}, stamping a rank-up if it crosses a tier. */
    void earn(int amount, int tick) {
        Snapshot before = this.snapshot;
        int xp = before.xp() + amount;
        boolean rankedUp = TowerRank.of(xp) != TowerRank.of(before.xp());
        this.snapshot = new Snapshot(xp, rankedUp ? tick : before.rankedUpAt());
    }

    /** Correlated, so they cross threads together. */
    private record Snapshot(int xp, int rankedUpAt) {
    }
}
