package td.ui.render;

import java.util.Optional;

/** One line of an {@link EnemySheet}. */
public sealed interface SheetLine {

    /** The enemy's body glyph and name, with its rank badge and rank name on the right. */
    record Header(Palette body, RankBadge badge, String name, String rank) implements SheetLine {
    }

    /**
     * A health bar with {@code value} on its right.
     *
     * @param gone killed or leaked: the bar dims and {@code value} says which
     */
    record HealthBar(int health, int maxHealth, String value, boolean gone) implements SheetLine {
    }

    /**
     * A glyph, a label and a right-aligned value. A {@code tone} colours the glyph and the label,
     * the same colour the board draws that trait or effect in.
     *
     * @param value empty when the label says everything
     */
    record Row(Glyph glyph, Optional<Palette> tone, String label, String value) implements SheetLine {

        /** A permanent trait: a hollow diamond, as under the enemy on the board. */
        public static Row trait(Palette tone, String label, String value) {
            return new Row(Glyph.HOLLOW_DIAMOND, Optional.of(tone), label, value);
        }

        /** A timed effect: a filled diamond, as above the enemy on the board. */
        public static Row effect(Palette tone, String label, String value) {
            return new Row(Glyph.FILLED_DIAMOND, Optional.of(tone), label, value);
        }

        /** A stat nothing on the board marks. */
        public static Row plain(Glyph glyph, String label, String value) {
            return new Row(glyph, Optional.empty(), label, value);
        }
    }

    /** Dimmed running text that wraps, such as a description. */
    record Prose(String text) implements SheetLine {
    }

    /** A small vertical gap between groups. */
    record Gap() implements SheetLine {
    }

    enum Glyph {
        HOLLOW_DIAMOND,
        FILLED_DIAMOND,
        DOT,
        CHEVRON
    }
}
