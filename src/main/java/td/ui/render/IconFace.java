package td.ui.render;

import java.util.OptionalDouble;

/**
 * What one term icon shows, without AWT: its art, its colours and its live state.
 *
 * @param pictogram the art's key; a key without art paints {@code monogram}
 * @param corner    bottom-right text: a stack count or a node's progress; empty for none
 * @param numeral   top-left text: a perk line's level; empty for none
 * @param elapsed   the share of its clock gone, 0 to 1, shown as a dim sweep; 0 for none
 * @param bar       how full its pool or level is, 0 to 1; empty for no bar
 * @param below     the label under the face, whole seconds left; empty for none
 */
public record IconFace(String pictogram, String monogram, Palette tone, Palette frame, String corner, String numeral,
                       float elapsed, OptionalDouble bar, Ward ward, boolean faded, String below) {

    /** What keeps the term out: nothing, all of it, or a share. */
    public enum Ward {
        NONE,
        IMMUNE,
        RESISTED
    }

    public static IconFace of(String pictogram, String monogram, Palette tone, Palette frame) {
        return new IconFace(pictogram, monogram, tone, frame, "", "", 0f, OptionalDouble.empty(), Ward.NONE, false, "");
    }

    public IconFace withCorner(String corner) {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, corner, this.numeral, this.elapsed,
                this.bar, this.ward, this.faded, this.below);
    }

    public IconFace withNumeral(String numeral) {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, this.corner, numeral, this.elapsed,
                this.bar, this.ward, this.faded, this.below);
    }

    public IconFace withElapsed(float elapsed) {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, this.corner, this.numeral, elapsed,
                this.bar, this.ward, this.faded, this.below);
    }

    public IconFace withBar(double bar) {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, this.corner, this.numeral,
                this.elapsed, OptionalDouble.of(bar), this.ward, this.faded, this.below);
    }

    public IconFace withWard(Ward ward) {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, this.corner, this.numeral,
                this.elapsed, this.bar, ward, this.faded, this.below);
    }

    /** Painted at a third of its alpha: only its diminishing returns are left. */
    public IconFace asFaded() {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, this.corner, this.numeral,
                this.elapsed, this.bar, this.ward, true, this.below);
    }

    public IconFace withBelow(String below) {
        return new IconFace(this.pictogram, this.monogram, this.tone, this.frame, this.corner, this.numeral,
                this.elapsed, this.bar, this.ward, this.faded, below);
    }
}
