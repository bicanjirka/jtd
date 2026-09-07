package td.ui;

import td.cell.Cell;
import td.ui.render.CellDraw;

/**
 * Describes a cell's placement/selection highlight. Only one {@link Cell}
 * implementation exists, so unlike towers/enemies this needs no visitor -
 * just plain-data getters read here.
 */
final class CellFrameBuilder {

    private CellFrameBuilder() {
    }

    /** Returns {@code null} for a cell with nothing to draw ({@link Cell.highlightType#none}). */
    static CellDraw build(Cell cell) {
        if (cell.getHighlight() == Cell.highlightType.none) {
            return null;
        }
        return new CellDraw(cell.getX(), cell.getY(), cell.getHighlight(), cell.buildable(), cell.getHighlightRange());
    }
}
