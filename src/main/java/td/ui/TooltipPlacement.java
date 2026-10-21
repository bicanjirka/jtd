package td.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/** Where a tooltip goes: left of its anchor, toward the board, else right; top-aligned; inside the window. */
final class TooltipPlacement {

    static final int GAP = 4;
    static final int MARGIN = 6;

    private TooltipPlacement() {
    }

    static Point place(Rectangle anchor, Dimension size, Rectangle window) {
        int x = anchor.x - GAP - size.width;
        if (x < window.x + MARGIN) {
            x = anchor.x + anchor.width + GAP;
        }
        x = Math.clamp(x, window.x + MARGIN,
                Math.max(window.x + MARGIN, window.x + window.width - MARGIN - size.width));
        int y = Math.clamp(anchor.y, window.y + MARGIN,
                Math.max(window.y + MARGIN, window.y + window.height - MARGIN - size.height));
        return new Point(x, y);
    }
}
