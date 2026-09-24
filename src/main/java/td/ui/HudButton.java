package td.ui;

import javax.swing.JButton;

import java.awt.Graphics;
import java.io.Serial;

/** A HUD push button, painted by {@link Hud}. */
final class HudButton extends JButton {

    @Serial
    private static final long serialVersionUID = 1L;

    HudButton(String text) {
        super(text);
        Hud.styleControl(this);
    }

    /** Does not call {@code super}: the look-and-feel must not paint this control. */
    @Override
    protected void paintComponent(Graphics g) {
        Hud.paintControl(g, this);
    }
}
