package td.tower;

/**
 * What a tower has earned and done so far: what a saved game keeps of it beyond what it bought.
 *
 * @param xpCarried the fraction of a point of XP a bonus left over, which the next earning adds to
 */
public record TowerTally(int xp, int deeds, long damageDealt, int kills, float xpCarried) {

    public static TowerTally of(int xp, int deeds, long damageDealt, int kills) {
        return new TowerTally(xp, deeds, damageDealt, kills, 0f);
    }

    public TowerTally withXpCarried(float carried) {
        return new TowerTally(this.xp, this.deeds, this.damageDealt, this.kills, carried);
    }
}
