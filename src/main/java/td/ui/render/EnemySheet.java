package td.ui.render;

import java.util.List;

/** An enemy's info-panel content as lines to lay out, without AWT, like a frame's draw commands. */
public record EnemySheet(List<SheetLine> lines) {

    public EnemySheet {
        lines = List.copyOf(lines);
    }
}
