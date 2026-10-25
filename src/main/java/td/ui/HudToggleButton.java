package td.ui;

import td.util.ThreadConfined;

import javax.swing.JToggleButton;

import java.awt.Dimension;
import java.awt.Graphics;
import java.io.Serial;

/**
 * A HUD button that stays pressed, painted by {@link Hud} like {@link HudButton} plus a selected
 * state.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class HudToggleButton extends JToggleButton {

    @Serial
    private static final long serialVersionUID = 1L;

    private ControlFace face = ControlFace.button();

    HudToggleButton() {
        Hud.styleControl(this);
    }

    ControlFace face() {
        return this.face;
    }

    void setFace(ControlFace face) {
        if (face.equals(this.face)) {
            return;
        }
        boolean resized = face.widthDiffersFrom(this.face);
        this.face = face;
        if (resized) {
            this.revalidate();
        }
        this.repaint();
    }

    /** Wide enough for a chip beside the text, when the face keeps one. */
    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (this.face.makesRoomForChip() && !this.isPreferredSizeSet()) {
            size.width += Hud.chipRoom(this, this.face.chip());
        }
        return size;
    }

    /** Does not call {@code super}: the look-and-feel must not paint this control. */
    @Override
    protected void paintComponent(Graphics g) {
        Hud.paintControl(g, this, this.face);
    }
}
