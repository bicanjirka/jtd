package td.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Where a tooltip goes: left of its anchor, toward the board, else right; opening down from the anchor's bottom, or
 * up from its top when there is no room below; inside the window. A source at the top of the window opens its tooltip
 * {@linkplain #below below} instead.
 */
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
        int y = anchor.y + anchor.height;
        if (y + size.height > window.y + window.height - MARGIN) {
            y = anchor.y - size.height;
        }
        y = Math.clamp(y, window.y + MARGIN,
                Math.max(window.y + MARGIN, window.y + window.height - MARGIN - size.height));
        return new Point(x, y);
    }

    /** Under its anchor, left edges aligned, inside the window: for a source in a strip across the top. */
    static Point below(Rectangle anchor, Dimension size, Rectangle window) {
        int x = Math.clamp(anchor.x, window.x + MARGIN,
                Math.max(window.x + MARGIN, window.x + window.width - MARGIN - size.width));
        int y = Math.clamp(anchor.y + anchor.height + GAP, window.y + MARGIN,
                Math.max(window.y + MARGIN, window.y + window.height - MARGIN - size.height));
        return new Point(x, y);
    }
}
