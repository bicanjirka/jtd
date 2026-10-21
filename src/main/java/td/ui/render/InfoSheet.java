package td.ui.render;

import java.util.List;
import java.util.Optional;

/** An enemy's info-panel content as lines to lay out, without AWT, like a frame's draw commands. */
public record InfoSheet(List<SheetLine> lines) {

    public InfoSheet {
        lines = List.copyOf(lines);
    }

    /** The tooltip an icon or row of this sheet opens under {@code key}. */
    public Optional<InfoSheet> tooltipFor(TooltipKey key) {
        return this.lines.stream().map(line -> line.tooltipFor(key)).flatMap(Optional::stream).findFirst();
    }
}
