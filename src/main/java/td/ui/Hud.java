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
 * The HUD's look: palette, fonts, panel border, and how every control paints itself.
 * <p>
 * <strong>Controls paint themselves on purpose.</strong> Platform look-and-feels differ between
 * operating systems, and their disabled text has come out invisible on these dark panels.
 * {@link #paintControl} draws everything itself; the look-and-feel's listeners stay, so pressed,
 * rollover and selected states still work. New controls are {@link HudButton} or
 * {@link HudToggleButton}.
 */
public final class Hud {

    static final Color BACKGROUND = Color.BLACK;
    static final Color FOREGROUND = new Color(220, 255, 220);
    static final Font LABEL_FONT = new Font("Dialog", Font.PLAIN, 11);
    static final Font GLYPH_FONT = new Font("Dialog", Font.PLAIN, 14);
    static final Color BORDER_IDLE = new Color(78, 104, 78);
    private static final Color BORDER_DISABLED = new Color(48, 60, 48);
    private static final Color TEXT_DISABLED = new Color(96, 112, 96);
    private static final Color FILL_HOVER = new Color(26, 38, 26);
    private static final Color FILL_PRESSED = new Color(46, 66, 46);
    private static final Color FILL_SELECTED = new Color(38, 56, 38);
    /** Padding inside a control, since {@link #paintControl} draws the border. */
    private static final Border CONTROL_PADDING = new EmptyBorder(3, 8, 3, 8);
    /** Inset and minimum gap of a row-style face's two parts. */
    private static final int ROW_PADDING = 6;
    private static final int ROW_GAP = 8;
    private static final String ELLIPSIS = "…";

    private Hud() {
    }

    /**
     * The one-pixel outline every panel and control shares. Not an etched border, whose shading
     * varies by platform.
     */
    public static Border outlineBorder() {
        return BorderFactory.createLineBorder(BORDER_IDLE);
    }

    /** The titled border every HUD panel wears. */
    public static Border panelBorder(String title) {
        return BorderFactory.createTitledBorder(outlineBorder(), title, TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION, LABEL_FONT, FOREGROUND);
    }

    /** Shared control setup: colours, font, padding, and no look-and-feel chrome. */
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
     * Draws a control: a flat fill reacting to the mouse, a one-pixel border, and its icon or else
     * its text, centred.
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

    /**
     * Centred text, or with a tab, a row like the info pane's: the part before it on the left, and the
     * part after it on the right.
     */
    private static void paintCentredText(Graphics2D g2, AbstractButton button, int width, int height, boolean enabled) {
        String text = button.getText();
        if (text == null || text.isEmpty()) {
            return;
        }
        g2.setFont(button.getFont());
        FontMetrics metrics = g2.getFontMetrics();
        int baseline = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g2.setColor(enabled ? button.getForeground() : TEXT_DISABLED);
        int tab = text.indexOf('	');
        if (tab < 0) {
            g2.drawString(text, (width - metrics.stringWidth(text)) / 2, baseline);
            return;
        }
        RowFace face = rowFace(text.substring(0, tab), text.substring(tab + 1), width, metrics);
        g2.drawString(face.right(), width - ROW_PADDING - metrics.stringWidth(face.right()), baseline);
        g2.drawString(face.left(), ROW_PADDING, baseline);
    }

    /** The two parts of a row-style face as they are drawn. */
    record RowFace(String left, String right) {
    }

    /**
     * Fits a row-style face into {@code width}. The left part, a name, stays whole; the right part
     * gives way first, keeping its end, where a progress count sits. Only when the left part does not
     * fit alone is it shortened, and then the right part is dropped.
     */
    static RowFace rowFace(String left, String right, int width, FontMetrics metrics) {
        int room = width - 2 * ROW_PADDING;
        int rightRoom = room - metrics.stringWidth(left) - ROW_GAP;
        if (rightRoom >= metrics.stringWidth(right)) {
            return new RowFace(left, right);
        }
        if (rightRoom > metrics.stringWidth(ELLIPSIS)) {
            return new RowFace(left, fittedKeepingEnd(right, rightRoom, metrics));
        }
        return new RowFace(fitted(left, room, metrics), "");
    }

    private static String fittedKeepingEnd(String text, int available, FontMetrics metrics) {
        String shortened = text;
        while (!shortened.isEmpty() && metrics.stringWidth(ELLIPSIS + shortened) > available) {
            shortened = shortened.substring(1);
        }
        return ELLIPSIS + shortened.stripLeading();
    }

    private static String fitted(String text, int available, FontMetrics metrics) {
        if (metrics.stringWidth(text) <= available) {
            return text;
        }
        String shortened = text;
        while (!shortened.isEmpty() && metrics.stringWidth(shortened + "…") > available) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened.stripTrailing() + "…";
    }
}
