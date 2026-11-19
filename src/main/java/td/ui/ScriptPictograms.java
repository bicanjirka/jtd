package td.ui;

import java.util.List;
import java.util.Map;

/**
 * What a wave script names besides affixes: its shapes, keyed by {@code ShapeKeyword.key()}, and one glyph every enemy
 * shares. Each spec is {@link Pictogram#parse} input; the path runs up the grid, as a slot walks it.
 */
final class ScriptPictograms {

    /** Every enemy's glyph in the script editor's list. */
    static final String ENEMY = "script.enemy";

    static final List<Map.Entry<String, String>> SPECS = List.of(
            // Flank: one at each side of the path
            Map.entry("shape.flank", "T M12 2.4V21.6 ; F O4.4 12 3.2 O19.6 12 3.2"),
            // Swarm: many small bodies
            Map.entry("shape.swarm", "=ability.swarm"),
            // Line: a row across the path
            Map.entry("shape.line", "T M12 2.4V21.6 ; F O3.6 12 2.6 O12 12 2.6 O20.4 12 2.6"),
            // Column: close one behind another
            Map.entry("shape.column", "F O12 4.2 2.5 O12 9.4 2.5 O12 14.6 2.5 O12 19.8 2.5"),
            // Drip: far apart, one behind another
            Map.entry("shape.drip", "F O12 4.6 3 O12 19.4 3 ; D O12 12 1.4"),
            // An enemy: head and shoulders
            Map.entry(ENEMY, "F O12 7.4 4.4 ; F M3.8 21.4C3.8 15.6 7.4 12.8 12 12.8C16.6 12.8 20.2 15.6 20.2 21.4Z"));

    private ScriptPictograms() {
    }
}
