package td.ui;

import td.cell.Cell;
import td.cell.CellGrid;
import td.cell.CellHighlight;
import td.economy.EconomyLedger;
import td.ui.render.CellDraw;

/** Describes the placement or selection highlight on its cell. */
final class CellFrameBuilder {

    private CellFrameBuilder() {
    }

    /**
     * {@code null} when nothing is highlighted, or the highlight names a cell of a grid since replaced.
     * {@code economy} decides whether a placement hover is affordable.
     */
    static CellDraw build(CellHighlight highlight, CellGrid grid, EconomyLedger economy) {
        if (highlight.kind() == CellHighlight.Kind.NONE || !grid.contains(highlight.cellX(), highlight.cellY())) {
            return null;
        }
        Cell cell = grid.at(highlight.cellX(), highlight.cellY());
        return new CellDraw(cell.getX(), cell.getY(), highlight.kind(), cell.buildable(), highlight.rangeCells(),
                economy.canPay(highlight.price()));
    }
}
