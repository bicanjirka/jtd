package td.ui.render;

import java.util.List;

/**
 * The dev cell grid: a line along every cell's right and bottom edge, and a fill on each cell a
 * path blocks, at its top-left pixel.
 */
public record CellGridDraw(List<BlockedCell> blockedCells) {

    public CellGridDraw {
        blockedCells = List.copyOf(blockedCells);
    }

    public record BlockedCell(int x, int y) {
    }
}
