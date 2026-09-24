package td.ui;

import javax.swing.JToggleButton;

import java.awt.Graphics;
import java.io.Serial;

/**
 * A HUD button that stays pressed, painted by {@link Hud} like {@link HudButton} plus a selected
 * state.
 */
final class HudToggleButton extends JToggleButton {

    @Serial
    private static final long serialVersionUID = 1L;

    HudToggleButton() {
        Hud.styleControl(this);
    }

    /** Does not call {@code super}: the look-and-feel must not paint this control. */
    @Override
    protected void paintComponent(Graphics g) {
        Hud.paintControl(g, this);
    }
}
