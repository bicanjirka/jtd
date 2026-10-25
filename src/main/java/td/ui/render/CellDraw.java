package td.ui.render;

import td.cell.Cell;

/**
 * A cell's highlight; cells without one produce no command. {@code affordable} says whether the credits cover the
 * tower a placement hover shows.
 */
public record CellDraw(int x, int y, Cell.HighlightType highlight, boolean buildable, float rangeCells,
                       boolean affordable) {
}
