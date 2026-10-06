package td.tower;

/**
 * The XP tiers a tower climbs, each named at the XP that opens it. Purely visual: a rank grants
 * nothing, it shows how much of the fight the tower has seen.
 */
public enum TowerRank {
    RECRUIT(0),
    SEASONED(50),
    EXPERT(150),
    HERO(300);

    private final int xp;

    TowerRank(int xp) {
        this.xp = xp;
    }

    /** The XP that opens this rank. */
    public int xp() {
        return this.xp;
    }

    public static TowerRank of(int xp) {
        TowerRank reached = RECRUIT;
        for (TowerRank rank : values()) {
            if (xp >= rank.xp) {
                reached = rank;
            }
        }
        return reached;
    }
}
