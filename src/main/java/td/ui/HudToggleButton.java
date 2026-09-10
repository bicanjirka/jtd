package td.ui;

import javax.swing.JToggleButton;
import java.awt.Graphics;
import java.io.Serial;

/**
 * A control in the game's HUD that stays pressed, for a mode the player turns on and off -
 * today only picking a tower to place. Identical in appearance to {@link HudButton} apart from
 * showing its selected state, so the toolbar does not look like a different kind of thing from
 * the transport controls.
 */
final class HudToggleButton extends JToggleButton {

    @Serial
    private static final long serialVersionUID = 1L;

    HudToggleButton() {
        Hud.styleControl(this);
    }

    /** Deliberately does not call {@code super}: the look-and-feel must not paint this control. */
    @Override
    protected void paintComponent(Graphics g) {
        Hud.paintControl(g, this);
    }
}
