package td.ui;

import td.ui.render.SheetTooltip;

import java.awt.Rectangle;

/** A part of a source the pointer rests on, in the source's coordinates, and the tooltip it opens. */
record TooltipSpot(SheetTooltip tooltip, Rectangle bounds) {

    TooltipSpot {
        bounds = new Rectangle(bounds);
    }
}
