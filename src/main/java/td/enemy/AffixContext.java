package td.enemy;

/**
 * What an affix measures itself against, so it means as much on a small enemy as on a large one.
 *
 * @param rank               the rank the slot spawns at
 * @param memberHealthFactor turns a definition's base health into the health one member of the slot is sent with:
 *                           the shape's health multiplier over the health divisor
 */
public record AffixContext(Rank rank, float memberHealthFactor) {

    /** The health one member of a slot of {@code definition} is sent with, never below one point. */
    public int memberHealthPoints(EnemyDefinition definition) {
        return Math.max(1, Math.round(definition.baseHealth() * this.memberHealthFactor));
    }
}
