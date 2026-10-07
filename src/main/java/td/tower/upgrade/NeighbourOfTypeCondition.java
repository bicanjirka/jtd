package td.tower.upgrade;

import td.board.BoardGeometry;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

/** Satisfied once another tower of {@code type} stands in one of the 8 cells around this tower. */
public record NeighbourOfTypeCondition(TowerFactory.Type type) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        BoardGeometry board = context.getBoard();
        int cellX = board.cellX(tower.getX());
        int cellY = board.cellY(tower.getY());
        return context.towers().all().stream()
                .filter(other -> other != tower && other.getType() == this.type)
                .anyMatch(other -> Math.abs(board.cellX(other.getX()) - cellX) <= 1
                        && Math.abs(board.cellY(other.getY()) - cellY) <= 1);
    }

    @Override
    public String describe() {
        return "next to another " + this.type.displayName();
    }
}
