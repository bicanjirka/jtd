package td.ui;

import java.awt.Color;
import java.util.Optional;

/**
 * What a control shows besides its text or icon: its key chip, and for a tile the accent its border and chip take.
 * A control with no accent is a button, square and neutral; one with an accent is a tile, something to buy or pick,
 * with rounded corners. A dimmed tile is one the player can't afford now.
 *
 * @param chip    the key's label on the chip, empty for no chip
 * @param overlay whether the chip shows only for a moment (the dev panel's, while Alt is held), drawn over the face
 *                without the control making room for it, so nothing moves when it comes and goes
 * @param accent  a tile's colour; empty for a button
 * @param dimmed  whether a tile's border and chip are dimmed
 * @param selectedBorder a selected button's border; empty for the plain text colour
 */
record ControlFace(String chip, boolean overlay, Optional<Color> accent, boolean dimmed, Optional<Color> selectedBorder) {

    static ControlFace button() {
        return new ControlFace("", false, Optional.empty(), false, Optional.empty());
    }

    static ControlFace tile(Color accent) {
        return new ControlFace("", false, Optional.of(accent), false, Optional.empty());
    }

    /** A chip the control always shows and makes room for. */
    ControlFace withChip(String label) {
        return new ControlFace(label, false, this.accent, this.dimmed, this.selectedBorder);
    }

    /** A chip drawn over the face for a moment; empty takes it away. */
    ControlFace withOverlayChip(String label) {
        return new ControlFace(label, true, this.accent, this.dimmed, this.selectedBorder);
    }

    ControlFace withDimmed(boolean dim) {
        return new ControlFace(this.chip, this.overlay, this.accent, dim, this.selectedBorder);
    }

    ControlFace withSelectedBorder(Color border) {
        return new ControlFace(this.chip, this.overlay, this.accent, this.dimmed, Optional.of(border));
    }

    /** Whether the control widens to fit its chip beside its text. */
    boolean makesRoomForChip() {
        return this.hasChip() && !this.overlay && !this.isTile();
    }

    /** Whether a control changing from {@code before} to this face changes its preferred width. */
    boolean widthDiffersFrom(ControlFace before) {
        return this.makesRoomForChip() != before.makesRoomForChip()
                || (this.makesRoomForChip() && !this.chip.equals(before.chip));
    }

    boolean isTile() {
        return this.accent.isPresent();
    }

    boolean hasChip() {
        return !this.chip.isEmpty();
    }
}
