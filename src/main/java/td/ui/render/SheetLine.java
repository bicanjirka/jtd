package td.ui.render;

import java.util.Optional;

/** One line of an {@link InfoSheet}. */
public sealed interface SheetLine {

    /** The enemy's body glyph and name, with its rank badge and rank name on the right. */
    record EnemyHeader(Palette body, RankBadge badge, String name, String rank) implements SheetLine {
    }

    /**
     * A glyph and a bold name, with a value on the right: a tower and its price, or an upgrade
     * node and its price.
     */
    record Title(Glyph glyph, Palette tone, String name, String value) implements SheetLine {
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
     * @param trend whether the value is better or worse than the thing's own base
     */
    record Row(Glyph glyph, Optional<Palette> tone, String label, String value, Trend trend) implements SheetLine {

        /** A permanent trait: a hollow diamond, as under the enemy on the board. */
        public static Row trait(Palette tone, String label, String value) {
            return toned(Glyph.HOLLOW_DIAMOND, tone, label, value);
        }

        /** A timed effect: a filled diamond, as above the enemy on the board. */
        public static Row effect(Palette tone, String label, String value) {
            return toned(Glyph.FILLED_DIAMOND, tone, label, value);
        }

        /** A stat nothing on the board marks. */
        public static Row plain(Glyph glyph, String label, String value) {
            return new Row(glyph, Optional.empty(), label, value, Trend.NONE);
        }

        /** Any glyph, with it and the label in {@code tone}'s colour. */
        public static Row toned(Glyph glyph, Palette tone, String label, String value) {
            return new Row(glyph, Optional.of(tone), label, value, Trend.NONE);
        }

        public Row withTrend(Trend trend) {
            return new Row(this.glyph, this.tone, this.label, this.value, trend);
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
        CHEVRON,
        /** A range or a radius. */
        RING,
        /** The four-point spark the board draws where a crit lands. */
        SPARK,
        SKULL,
        /** A slot pip, as under an upgraded tower on the board. */
        PIP,
        CHECK,
        CROSS,
        /** An exclusive choice: what a pick locks out, or what a made one was chosen over. */
        LOCK,
        /** The body shape of the tower whose body palette is the row's tone. */
        TOWER_BODY
    }

    /** How a value compares with its own base. */
    enum Trend {
        NONE,
        BETTER,
        WORSE
    }
}
