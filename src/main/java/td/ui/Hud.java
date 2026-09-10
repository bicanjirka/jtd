package td.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * The one place the game's HUD decides what it looks like: its palette, its fonts, the border
 * its panels wear, and how every clickable control paints itself.
 * <p>
 * <strong>The controls paint themselves on purpose.</strong> Swing's default button chrome is
 * the platform look-and-feel's, so the same build looks different on Windows, macOS and Metal,
 * and its disabled-text colour is chosen for a light button face - against this game's black
 * panels it has repeatedly come out invisible. {@link #paintControl} therefore draws the
 * background, border and content itself and never delegates to the look-and-feel. The look-and-feel's
 * listeners are left installed, so pressed/rollover/selected still track the mouse normally;
 * only the painting is taken over.
 * <p>
 * Every control shares this one style. A new control belongs in {@link HudButton} or
 * {@link HudToggleButton}, not in a bare {@code JButton} styled by hand.
 */
public final class Hud {

    static final Color BACKGROUND = Color.BLACK;
    static final Color FOREGROUND = new Color(220, 255, 220);

    private static final Color BORDER_IDLE = new Color(78, 104, 78);
    private static final Color BORDER_DISABLED = new Color(48, 60, 48);
    private static final Color TEXT_DISABLED = new Color(96, 112, 96);
    private static final Color FILL_HOVER = new Color(26, 38, 26);
    private static final Color FILL_PRESSED = new Color(46, 66, 46);
    private static final Color FILL_SELECTED = new Color(38, 56, 38);

    static final Font LABEL_FONT = new Font("Dialog", Font.PLAIN, 11);
    static final Font GLYPH_FONT = new Font("Dialog", Font.PLAIN, 14);

    /** Padding inside a control, since {@link #paintControl} draws the border itself. */
    private static final Border CONTROL_PADDING = new EmptyBorder(3, 8, 3, 8);

    private Hud() {
    }

    /**
     * The plain outline shared by every panel and every control - one pixel, one colour. An
     * explicit line rather than {@code createEtchedBorder}, whose shading is the look-and-feel's
     * to choose and so differs between platforms.
     */
    public static Border outlineBorder() {
        return BorderFactory.createLineBorder(BORDER_IDLE);
    }

    /** The titled border every HUD panel wears, drawn with the same outline the controls use. */
    public static Border panelBorder(String title) {
        return BorderFactory.createTitledBorder(outlineBorder(), title, TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, LABEL_FONT, FOREGROUND);
    }

    /** Applies the shared control setup - colours, font, padding, and suppressing the look-and-feel's own chrome. */
    static void styleControl(AbstractButton button) {
        button.setBackground(BACKGROUND);
        button.setForeground(FOREGROUND);
        button.setFont(LABEL_FONT);
        button.setBorder(CONTROL_PADDING);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.setOpaque(true);
    }

    /**
     * Draws a control: a flat fill that reacts to the mouse, a one-pixel border, and either the
     * button's icon or its text centred inside. A control carries one or the other - the icon
     * wins if both are set, which no control here does.
     */
    static void paintControl(Graphics g, AbstractButton button) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = button.getWidth();
        int height = button.getHeight();
        ButtonModel model = button.getModel();
        boolean enabled = button.isEnabled();

        g2.setColor(fillFor(model, enabled));
        g2.fillRect(0, 0, width, height);
        g2.setColor(enabled ? BORDER_IDLE : BORDER_DISABLED);
        g2.drawRect(0, 0, width - 1, height - 1);

        Icon icon = iconFor(button, enabled);
        if (icon != null) {
            icon.paintIcon(button, g2, (width - icon.getIconWidth()) / 2, (height - icon.getIconHeight()) / 2);
        } else {
            paintCentredText(g2, button, width, height, enabled);
        }
        g2.dispose();
    }

    private static Color fillFor(ButtonModel model, boolean enabled) {
        if (!enabled) {
            return BACKGROUND;
        }
        if (model.isPressed() && model.isArmed()) {
            return FILL_PRESSED;
        }
        if (model.isSelected()) {
            return FILL_SELECTED;
        }
        return model.isRollover() ? FILL_HOVER : BACKGROUND;
    }

    private static Icon iconFor(AbstractButton button, boolean enabled) {
        if (enabled) {
            return button.getIcon();
        }
        return button.getDisabledIcon() != null ? button.getDisabledIcon() : button.getIcon();
    }

    private static void paintCentredText(Graphics2D g2, AbstractButton button, int width, int height, boolean enabled) {
        String text = button.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        g2.setFont(button.getFont());
        FontMetrics metrics = g2.getFontMetrics();
        int x = (width - metrics.stringWidth(text)) / 2;
        int baseline = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setColor(enabled ? button.getForeground() : TEXT_DISABLED);
        g2.drawString(text, x, baseline);
    }
}
