package td.tower;

/**
 * What a tower has earned and done so far: what a saved game keeps of it beyond what it bought.
 *
 * @param xpCarried the fraction of a point of XP a bonus left over, which the next earning adds to
 */
public record TowerTally(int xp, long damageDealt, int kills, float xpCarried) {

    public static TowerTally of(int xp, long damageDealt, int kills) {
        return new TowerTally(xp, damageDealt, kills, 0f);
    }

    public TowerTally withXpCarried(float carried) {
        return new TowerTally(this.xp, this.damageDealt, this.kills, carried);
    }
}
