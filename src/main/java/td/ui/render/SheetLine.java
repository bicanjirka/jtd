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
     * A health bar, with a strip of the shields it holds under it, and on its right what the enemy is worth while it
     * lives or what became of it.
     *
     * @param tallies the bounty and lives, each a face and its number; shown while it lives
     * @param fate    killed, leaked or hatched: the bar dims and this says which
     * @param shields what its shields hold, in points; {@link Shields#none()} draws no strip
     */
    record HealthBar(int health, int maxHealth, List<Tally> tallies, Optional<String> fate, Shields shields)
            implements SheetLine {

        public HealthBar {
            tallies = List.copyOf(tallies);
        }

        /** A bar with no shield strip. */
        public HealthBar(int health, int maxHealth, List<Tally> tallies, Optional<String> fate) {
            this(health, maxHealth, tallies, fate, Shields.none());
        }

        public HealthBar withShields(Shields shields) {
            return new HealthBar(this.health, this.maxHealth, this.tallies, this.fate, shields);
        }
    }

    /**
     * What an enemy's shields hold, in points, by the damage they take.
     *
     * @param any          what the shields that take every type hold
     * @param physicalOnly what the shields that take only physical damage hold
     * @param magicOnly    what the shields that take only magic damage hold
     */
    record Shields(float any, float physicalOnly, float magicOnly) {

        private static final Shields NONE = new Shields(0f, 0f, 0f);

        public static Shields none() {
            return NONE;
        }

        public float total() {
            return this.any + this.physicalOnly + this.magicOnly;
        }
    }

    /** A painted face and the number beside it: {@code [$]5}. */
    record Tally(IconFace face, String value) {
    }

    /** A stat's mark and full name, bold: the head of a stat's hover. */
    record StatTitle(Glyph glyph, Optional<Palette> tone, Optional<IconFace> face, String name) implements SheetLine {
    }

    /**
     * A mark, a label and a right-aligned value with its unit and meaning. A {@code tone} colours the mark only, the
     * colour the board draws that thing in; a {@code face} is painted in place of the glyph.
     *
     * @param value   empty when the label says everything
     * @param unit    the dim word after the value; empty for none
     * @param meaning the dim words after the unit; empty for none
     * @param trend   whether the value is better or worse for the player than the thing's own base
     * @param arrow   which way the value moved from its base
     * @param tooltip what hovering the row opens, if anything
     */
    record Row(Glyph glyph, Optional<Palette> tone, Optional<IconFace> face, String label, String value, String unit,
               String meaning, Trend trend, Arrow arrow, Optional<SheetTooltip> tooltip) implements SheetLine {

        /** A permanent trait: a hollow diamond, as under the enemy on the board. */
        public static Row trait(Palette tone, String label, String value) {
            return toned(Glyph.HOLLOW_DIAMOND, tone, label, value);
        }

        /** A timed effect: a filled diamond, as above the enemy on the board. */
        public static Row effect(Palette tone, String label, String value) {
            return toned(Glyph.FILLED_DIAMOND, tone, label, value);
        }

        /** A row nothing on the board marks. */
        public static Row plain(Glyph glyph, String label, String value) {
            return new Row(glyph, Optional.empty(), Optional.empty(), label, value, "", "", Trend.NONE, Arrow.NONE,
                    Optional.empty());
        }

        /** Any glyph, in {@code tone}'s colour. */
        public static Row toned(Glyph glyph, Palette tone, String label, String value) {
            return new Row(glyph, Optional.of(tone), Optional.empty(), label, value, "", "", Trend.NONE, Arrow.NONE,
                    Optional.empty());
        }

        /** A term's face in place of a glyph. */
        public static Row faced(IconFace face, String label, String value) {
            return new Row(Glyph.DOT, Optional.empty(), Optional.of(face), label, value, "", "", Trend.NONE,
                    Arrow.NONE, Optional.empty());
        }

        public Row withTrend(Trend trend) {
            return new Row(this.glyph, this.tone, this.face, this.label, this.value, this.unit, this.meaning, trend,
                    this.arrow, this.tooltip);
        }

        public Row withArrow(Arrow arrow) {
            return new Row(this.glyph, this.tone, this.face, this.label, this.value, this.unit, this.meaning,
                    this.trend, arrow, this.tooltip);
        }

        /** The dim unit and meaning after the value. */
        public Row withGloss(String unit, String meaning) {
            return new Row(this.glyph, this.tone, this.face, this.label, this.value, unit, meaning, this.trend,
                    this.arrow, this.tooltip);
        }

        public Row withTooltip(TooltipKey key, InfoSheet sheet) {
            return new Row(this.glyph, this.tone, this.face, this.label, this.value, this.unit, this.meaning,
                    this.trend, this.arrow, Optional.of(new SheetTooltip(key, sheet)));
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
        TOWER_BODY,
        /** A ring with a dot at its centre: a blast around where a shot lands. */
        RING_CENTRE,
        /** A ring with a gap: what lies too close to reach. */
        BROKEN_RING,
        /** A slice of a circle: a cone. */
        WEDGE,
        /** A short arrow through a line: what a hit ignores. */
        PIERCE,
        /** A ring and a cross with a filled centre: what aims true. */
        CROSSHAIR_FILLED,
        /** A ring and a cross: what evades aim. */
        CROSSHAIR,
        /** The crit spark's outline: what blunts crits. */
        HOLLOW_SPARK,
        /** A shield's outline: what mitigates a share. */
        HOLLOW_SHIELD,
        /** A solid shield: what takes a fixed amount off. */
        FILLED_SHIELD,
        /** A shield with a crack: what lets more through. */
        CRACKED_SHIELD,
        /** A plus: what heals. */
        PLUS,
        /** A dot with a short trail: what flies. */
        COMET
    }

    /** Which way a value moved from its base. */
    enum Arrow {
        NONE,
        UP,
        DOWN
    }

    /** How a value compares with its own base. */
    enum Trend {
        NONE,
        BETTER,
        WORSE
    }
}
