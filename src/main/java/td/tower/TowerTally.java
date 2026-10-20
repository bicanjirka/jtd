package td.tower;

/** What a tower has earned and done so far: what a saved game keeps of it beyond what it bought. */
public record TowerTally(int xp, int deeds, long damageDealt, int kills) {
}
