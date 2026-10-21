package td.ui;

import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.TooltipKey;

import java.awt.Rectangle;

/** What the pointer rests on in a sheet, where it is, and the tooltip it opens. */
sealed interface HoverTarget {

    TooltipKey key();

    InfoSheet sheet();

    /** In the pane's coordinates. */
    Rectangle bounds();

    /** A term named in running text. */
    record Mention(TermText.Mention mention, Rectangle bounds) implements HoverTarget {

        public Mention {
            bounds = new Rectangle(bounds);
        }

        @Override
        public TooltipKey key() {
            return TooltipKey.ofTerm("term", this.mention.term());
        }

        @Override
        public InfoSheet sheet() {
            return TermSheets.of(this.mention);
        }
    }

    /** One face of an icon row. */
    record Icon(SheetIcon icon, Rectangle bounds) implements HoverTarget {

        public Icon {
            bounds = new Rectangle(bounds);
        }

        @Override
        public TooltipKey key() {
            return this.icon.key();
        }

        @Override
        public InfoSheet sheet() {
            return this.icon.tooltip();
        }
    }

    /** A whole row that opens a tooltip. */
    record Row(SheetLine.Row row, Rectangle bounds) implements HoverTarget {

        public Row {
            bounds = new Rectangle(bounds);
        }

        @Override
        public TooltipKey key() {
            return this.row.tooltip().orElseThrow().key();
        }

        @Override
        public InfoSheet sheet() {
            return this.row.tooltip().orElseThrow().sheet();
        }
    }
}
