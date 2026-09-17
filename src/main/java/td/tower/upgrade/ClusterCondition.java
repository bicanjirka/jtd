package td.tower.upgrade;

import td.board.BoardGeometry;
import td.tower.Tower;
import td.util.GameWorld;

/**
 * Satisfied once at least {@code requiredAdjacent} other towers occupy one of the 8 cells
 * surrounding this tower's own cell - a group built together, not any one tower's own
 * performance. Cell membership is derived from each tower's pixel centre
 * ({@link Tower#getX()}/{@link Tower#getY()}) the same way {@code TowerRoster} already
 * converts a tower's position to a cell for sell/clear.
 */
public record ClusterCondition(int requiredAdjacent) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        BoardGeometry board = context.getBoard();
        int cellX = board.cellX(tower.getX());
        int cellY = board.cellY(tower.getY());
        int adjacent = 0;
        for (Tower other : context.towers().all()) {
            if (other == tower) {
                continue;
            }
            int dx = board.cellX(other.getX()) - cellX;
            int dy = board.cellY(other.getY()) - cellY;
            if (Math.abs(dx) <= 1 && Math.abs(dy) <= 1) {
                adjacent++;
            }
        }
        return adjacent >= this.requiredAdjacent;
    }
}
