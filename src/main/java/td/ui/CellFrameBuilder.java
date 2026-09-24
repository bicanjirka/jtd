package td.ui;

import td.cell.Cell;
import td.ui.render.CellDraw;

/** Describes a cell's placement or selection highlight. */
final class CellFrameBuilder {

    private CellFrameBuilder() {
    }

    /** {@code null} when the cell has no highlight. */
    static CellDraw build(Cell cell) {
        if (cell.getHighlight() == Cell.HighlightType.NONE) {
            return null;
        }
        return new CellDraw(cell.getX(), cell.getY(), cell.getHighlight(), cell.buildable(), cell.getHighlightRange());
    }
}
