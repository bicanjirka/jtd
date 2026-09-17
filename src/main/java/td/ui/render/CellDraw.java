package td.ui.render;

import td.cell.Cell;

/**
 * A cell's placement/selection highlight. Only cells with a highlight worth
 * drawing produce one of these - {@link Cell.HighlightType#none} cells are
 * simply absent from {@link RenderFrame#cells()}.
 */
public record CellDraw(int x, int y, Cell.HighlightType highlight, boolean buildable, float rangeCells) {
}
