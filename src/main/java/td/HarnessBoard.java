package td;

import td.cell.CellGrid;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.ThreadConfined;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Where the headless harnesses put towers: the buildable cells ranked by how much path they reach. */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class HarnessBoard {

    /** A cell is worth the stretch of path within this many cells of it. */
    private static final double REACH_CELLS = 3.5;

    private HarnessBoard() {
    }

    /** A buildable cell and how much path is within reach of it. */
    record Cell(int x, int y, int near) {
    }

    /** Buildable cells, the ones with the most path within reach first. */
    static List<Cell> rankCells(GameEngine engine) {
        CellGrid grid = engine.cells();
        int scale = engine.getGameWorld().getBoard().scale();
        List<Vec2> pathPoints = engine.getGameWorld().level().paths().stream()
                .flatMap(path -> path.path().points().stream()).toList();
        double reach = REACH_CELLS * scale;
        List<Cell> cells = new ArrayList<>();
        for (int x = 0; x < grid.width(); x++) {
            for (int y = 0; y < grid.height(); y++) {
                if (grid.at(x, y).buildable()) {
                    double centreX = x * scale + scale / 2.0;
                    double centreY = y * scale + scale / 2.0;
                    int near = (int) pathPoints.stream()
                            .filter(point -> Math.hypot(point.x() - centreX, point.y() - centreY) <= reach).count();
                    cells.add(new Cell(x, y, near));
                }
            }
        }
        cells.sort(Comparator.comparingInt(Cell::near).reversed().thenComparingInt(Cell::x).thenComparingInt(Cell::y));
        return cells;
    }

    /** Builds {@code type} on {@code cell} through the engine's own placement, so the price rules apply. */
    static Optional<Tower> place(GameEngine engine, TowerFactory.Type type, Cell cell) {
        int scale = engine.getGameWorld().getBoard().scale();
        int pixelX = cell.x() * scale + scale / 2;
        int pixelY = cell.y() * scale + scale / 2;
        engine.startPlacing(type, 0f);
        engine.mouseClicked(pixelX, pixelY);
        if (!engine.cells().at(cell.x(), cell.y()).hasTower()) {
            return Optional.empty();
        }
        return engine.getTowers().stream().filter(tower -> tower.getX() == pixelX && tower.getY() == pixelY).findFirst();
    }
}
