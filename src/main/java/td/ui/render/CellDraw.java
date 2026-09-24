package td.ui.render;

import td.cell.Cell;

/** A cell's highlight; cells without one produce no command. */
public record CellDraw(int x, int y, Cell.HighlightType highlight, boolean buildable, float rangeCells) {
}
