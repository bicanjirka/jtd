package td.tower;

import td.util.TickRate;

/**
 * One tower's experience: the XP it earned from the bounty of every enemy it reached, when it last
 * ranked up, and how often it did its job (its deeds, which purpose gates count). XP and deeds
 * only climb. The game loop records; any thread reads the published snapshot.
 */
public final class TowerExperience {

    private static final int TICKS_PER_SECOND = Math.round(TickRate.TICKS_PER_SECOND);

    private volatile Snapshot snapshot = new Snapshot(0, -1, 0, -1);

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

    public int deeds() {
        return this.snapshot.deeds();
    }

    /** Adds {@code amount} XP during tick {@code tick}, stamping a rank-up if it crosses a tier. */
    void earn(int amount, int tick) {
        Snapshot before = this.snapshot;
        int xp = before.xp() + amount;
        boolean rankedUp = TowerRank.of(xp) != TowerRank.of(before.xp());
        this.snapshot = new Snapshot(xp, rankedUp ? tick : before.rankedUpAt(), before.deeds(), before.lastDeedAt());
    }

    /** A deed done by an attack: a second one in the same tick (another enemy it hit) doesn't count. */
    void countDeedOfAttack(int tick) {
        this.countDeedAfter(tick, 1);
    }

    /** A deed done over time: it counts at most once a second. */
    void countDeedOfSecond(int tick) {
        this.countDeedAfter(tick, TICKS_PER_SECOND);
    }

    private void countDeedAfter(int tick, int minimumTicksSinceLast) {
        Snapshot before = this.snapshot;
        if (before.lastDeedAt() >= 0 && tick - before.lastDeedAt() < minimumTicksSinceLast) {
            return;
        }
        this.snapshot = new Snapshot(before.xp(), before.rankedUpAt(), before.deeds() + 1, tick);
    }

    /** Correlated, so they cross threads together. */
    private record Snapshot(int xp, int rankedUpAt, int deeds, int lastDeedAt) {
    }
}
