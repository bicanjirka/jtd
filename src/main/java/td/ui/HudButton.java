package td.ui;

import javax.swing.JButton;
import java.awt.Graphics;
import java.io.Serial;

/**
 * A push control in the game's HUD. Paints itself through {@link Hud} rather than through the
 * platform look-and-feel - see that class for why - so it looks the same on every OS and
 * matches every other control.
 */
final class HudButton extends JButton {

    @Serial
    private static final long serialVersionUID = 1L;

    HudButton(String text) {
        super(text);
        Hud.styleControl(this);
    }

    /**
     * Deliberately does not call {@code super}: the look-and-feel must not paint this control.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Hud.paintControl(g, this);
    }
}
