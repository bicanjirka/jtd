package td.tower.targeting;

/**
 * What a tower tells a {@link TargetPriority} as it picks.
 *
 * @param cellSize the size of a board cell, in pixels
 * @param critHit  what its next hit deals on a crit, before armor and hit bonuses; 0 from a tower that doesn't say
 */
public record PickContext(float cellSize, int critHit) {

    /** A tower that tells only the board's scale. */
    public static PickContext of(float cellSize) {
        return new PickContext(cellSize, 0);
    }
}
