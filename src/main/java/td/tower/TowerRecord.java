package td.tower;

/**
 * What a tower carries from one wave into the next besides where it stands and what it owns: what it has earned, what
 * it was bought for, and what of it has been used. A saved game keeps it, and a rebuilt tower takes it up.
 *
 * @param tally what it has earned and done
 * @param paid  what the tower itself was bought for, its upgrades aside
 * @param use   what of it has been used, and so what sells back in full
 */
public record TowerRecord(TowerTally tally, int paid, TowerUse use) {
}
