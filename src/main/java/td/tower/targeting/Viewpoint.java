package td.tower.targeting;

/**
 * Where a tower looks from, for the perks that shape whom it may hit.
 *
 * @param x        its centre, in pixels
 * @param y        its centre, in pixels
 * @param range    its range, in pixels
 * @param cellSize the size of a board cell, in pixels
 */
public record Viewpoint(int x, int y, float range, int cellSize) {
}
