package td.ui.render;

import td.term.TermText;

import java.util.List;
import java.util.Optional;

/** One line of an {@link InfoSheet}. */
public sealed interface SheetLine {

    /** The tooltip this line opens under {@code key}, if it opens one. */
    default Optional<InfoSheet> tooltipFor(TooltipKey key) {
        return Optional.empty();
    }

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
     * @param tooltip what hovering the row opens, if anything
     */
    record Row(Glyph glyph, Optional<Palette> tone, String label, String value, Trend trend,
               Optional<SheetTooltip> tooltip) implements SheetLine {

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
            return new Row(glyph, Optional.empty(), label, value, Trend.NONE, Optional.empty());
        }

        /** Any glyph, with it and the label in {@code tone}'s colour. */
        public static Row toned(Glyph glyph, Palette tone, String label, String value) {
            return new Row(glyph, Optional.of(tone), label, value, Trend.NONE, Optional.empty());
        }

        public Row withTrend(Trend trend) {
            return new Row(this.glyph, this.tone, this.label, this.value, trend, this.tooltip);
        }

        public Row withTooltip(TooltipKey key, InfoSheet sheet) {
            return new Row(this.glyph, this.tone, this.label, this.value, this.trend,
                    Optional.of(new SheetTooltip(key, sheet)));
        }

        @Override
        public Optional<InfoSheet> tooltipFor(TooltipKey key) {
            return this.tooltip.filter(tooltip -> tooltip.key().equals(key)).map(SheetTooltip::sheet);
        }
    }

    /** Dimmed running text that wraps, such as a description. */
    record Prose(TermText text) implements SheetLine {
    }

    /**
     * Icon faces in a row, a label before them: the effects, abilities or perk lines of one kind. Each face opens
     * its own tooltip.
     */
    record IconRow(String label, List<SheetIcon> icons) implements SheetLine {

        public IconRow {
            icons = List.copyOf(icons);
        }

        @Override
        public Optional<InfoSheet> tooltipFor(TooltipKey key) {
            return this.icons.stream().filter(icon -> icon.key().equals(key)).map(SheetIcon::tooltip).findFirst();
        }
    }

    /** A face, a bold name in the face's tone, and a value on the right: the head of a tooltip. */
    record IconTitle(IconFace face, String name, Palette tone, String value) implements SheetLine {
    }

    /**
     * Running text that wraps, its mentions shown as icon and title.
     *
     * @param glyph a marker before the text, if any
     * @param tone  the marker's colour
     */
    record Sentence(TermText text, Optional<Glyph> glyph, Optional<Palette> tone) implements SheetLine {

        public static Sentence of(TermText text) {
            return new Sentence(text, Optional.empty(), Optional.empty());
        }

        public static Sentence marked(Glyph glyph, Palette tone, TermText text) {
            return new Sentence(text, Optional.of(glyph), Optional.of(tone));
        }
    }

    /** A thin line across the pane between groups. */
    record Divider() implements SheetLine {
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
