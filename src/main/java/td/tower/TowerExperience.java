package td.tower;

/**
 * One tower's experience: the XP it earned from the bounty of every enemy it reached. XP only climbs. The game loop
 * records; any thread reads it.
 */
public final class TowerExperience {

    private volatile int xp;

    public int xp() {
        return this.xp;
    }

    /** Adds {@code amount} XP; only the game loop earns, so the read and the write never race. */
    void earn(int amount) {
        this.xp = this.xp + amount;
    }

    /** Replaces the XP with a saved game's. */
    void restore(int xp) {
        this.xp = xp;
    }
}
