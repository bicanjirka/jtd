package td.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

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
    static final Color BORDER_DISABLED = new Color(48, 60, 48);
    static final Color BORDER_HOVER = new Color(127, 160, 127);
    static final Color TEXT_DISABLED = new Color(96, 112, 96);
    /** Secondary text: captions, hints, what is not the value itself. */
    static final Color TEXT_DIM = new Color(143, 168, 143);
    static final Color FILL_HOVER = new Color(26, 38, 26);
    private static final Color FILL_PRESSED = new Color(46, 66, 46);
    static final Color FILL_SELECTED = new Color(38, 56, 38);
    static final Color CHIP_BORDER = new Color(111, 143, 111);
    /** A price the player can't pay now. */
    static final Color SHORT = new Color(255, 107, 107);
    static final Font CHIP_FONT = new Font("Dialog", Font.BOLD, 8);
    static final Font PRICE_FONT = new Font("Dialog", Font.BOLD, 9);
    /** A tile's corner radius; a button is square. */
    static final float TILE_CORNER = 7f;
    /** Padding inside a control, since {@link #paintControl} draws the border. */
    private static final Border CONTROL_PADDING = new EmptyBorder(3, 8, 3, 8);
    private static final int CHIP_INSET = 2;
    private static final int CHIP_SIZE = 12;
    private static final int CHIP_PADDING = 2;
    private static final int CHIP_GAP = 2;
    private static final float CHIP_ARC = 6f;
    private static final float DIM_BORDER = 0.4f;
    private static final float DIM_CHIP = 0.45f;
    private static final float HOVER_LIGHTEN = 0.35f;
    /** Inset and minimum gap of a row-style face's two parts. */
    private static final int ROW_PADDING = 6;
    private static final int ICON_GAP = 4;
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
     * Draws a control: a flat fill reacting to the mouse, its border, its icon or else its text, centred, and its
     * key chip in the top-left corner. A button is square with a neutral border; a tile has rounded corners and its
     * accent for a border. Hover brightens the border, selected thickens a tile's. A button with an icon and a
     * row-style text (one with a tab) paints the icon first and the row beside it. Text keeps clear of a chip when
     * there is room beside it.
     */
    static void paintControl(Graphics g, AbstractButton button, ControlFace face) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = button.getWidth();
        int height = button.getHeight();
        ButtonModel model = button.getModel();
        boolean enabled = button.isEnabled();

        g2.setColor(BACKGROUND);
        g2.fillRect(0, 0, width, height);
        boolean thick = face.isTile() && enabled && model.isSelected();
        g2.setColor(fillFor(model, enabled));
        g2.fill(outline(face, width, height, 0f));
        g2.setColor(borderFor(face, model, enabled));
        g2.setStroke(new BasicStroke(thick ? 2f : 1f));
        g2.draw(outline(face, width, height, thick ? 1f : 0.5f));

        Icon icon = iconFor(button, enabled);
        String text = button.getText();
        if (icon != null && text != null && text.indexOf('	') >= 0) {
            icon.paintIcon(button, g2, ROW_PADDING, (height - icon.getIconHeight()) / 2);
            int taken = ROW_PADDING + icon.getIconWidth() + ICON_GAP;
            Graphics2D row = (Graphics2D) g2.create();
            row.translate(taken, 0);
            paintCentredText(row, button, width - taken, height, enabled);
            row.dispose();
        } else if (icon != null) {
            icon.paintIcon(button, g2, (width - icon.getIconWidth()) / 2, (height - icon.getIconHeight()) / 2);
        } else {
            int clear = face.hasChip() ? chipRoom(button, face.chip()) : 0;
            int textWidth = text == null ? 0 : button.getFontMetrics(button.getFont()).stringWidth(text);
            if (clear > 0 && textWidth <= width - 2 * clear) {
                paintCentredText(g2, button, width, height, enabled);
            } else if (clear > 0 && textWidth <= width - clear - CHIP_GAP) {
                Graphics2D beside = (Graphics2D) g2.create();
                beside.translate(clear, 0);
                paintCentredText(beside, button, width - clear - CHIP_GAP, height, enabled);
                beside.dispose();
            } else {
                paintCentredText(g2, button, width, height, enabled);
            }
        }
        if (face.hasChip()) {
            paintChip(g2, face.chip(), CHIP_INSET, CHIP_INSET, chipBorderFor(face, enabled), chipInkFor(face, enabled));
        }
        g2.dispose();
    }

    /** The room a chip takes from the left edge, its gap included. */
    static int chipRoom(Component control, String label) {
        return CHIP_INSET + chipWidth(control.getFontMetrics(CHIP_FONT), label) + CHIP_GAP;
    }

    private static int chipWidth(FontMetrics metrics, String label) {
        return Math.max(CHIP_SIZE, metrics.stringWidth(label) + 2 * CHIP_PADDING + 2);
    }

    /** An outlined keycap at ({@code x}, {@code y}): black inside, {@code border} round it, the label in {@code ink}. */
    static void paintChip(Graphics2D g2, String label, float x, float y, Color border, Color ink) {
        Graphics2D chip = (Graphics2D) g2.create();
        chip.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        chip.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        chip.setFont(CHIP_FONT);
        FontMetrics metrics = chip.getFontMetrics();
        float width = chipWidth(metrics, label);
        Shape cap = new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1, CHIP_SIZE - 1, CHIP_ARC, CHIP_ARC);
        chip.setColor(BACKGROUND);
        chip.fill(cap);
        chip.setColor(border);
        chip.setStroke(new BasicStroke(1f));
        chip.draw(cap);
        chip.setColor(ink);
        chip.drawString(label, x + (width - metrics.stringWidth(label)) / 2f,
                y + (CHIP_SIZE - metrics.getAscent() - metrics.getDescent()) / 2f + metrics.getAscent());
        chip.dispose();
    }

    /** A control's outline grown inward by {@code inset}: square for a button, rounded for a tile. */
    private static Shape outline(ControlFace face, int width, int height, float inset) {
        float w = width - 2 * inset;
        float h = height - 2 * inset;
        if (!face.isTile()) {
            return new Rectangle2D.Float(inset, inset, w, h);
        }
        float arc = 2 * Math.max(0f, TILE_CORNER - inset);
        return new RoundRectangle2D.Float(inset, inset, w, h, arc, arc);
    }

    private static Color borderFor(ControlFace face, ButtonModel model, boolean enabled) {
        boolean hot = model.isRollover() || model.isPressed();
        if (face.accent().isEmpty()) {
            if (!enabled) {
                return BORDER_DISABLED;
            }
            if (model.isSelected()) {
                return FOREGROUND;
            }
            return hot ? BORDER_HOVER : BORDER_IDLE;
        }
        Color accent = face.accent().get();
        if (!enabled) {
            return withAlpha(accent, DIM_BORDER / 2);
        }
        if (model.isSelected()) {
            return accent;
        }
        if (hot) {
            return face.dimmed() ? withAlpha(accent, 2 * DIM_BORDER) : mix(accent, Color.WHITE, HOVER_LIGHTEN);
        }
        return face.dimmed() ? withAlpha(accent, DIM_BORDER) : accent;
    }

    private static Color chipBorderFor(ControlFace face, boolean enabled) {
        if (!enabled) {
            return BORDER_DISABLED;
        }
        return face.accent().map(accent -> withAlpha(accent, face.dimmed() ? DIM_CHIP / 2 : 0.6f)).orElse(CHIP_BORDER);
    }

    private static Color chipInkFor(ControlFace face, boolean enabled) {
        if (!enabled) {
            return TEXT_DISABLED;
        }
        return face.accent().map(accent -> face.dimmed() ? withAlpha(accent, DIM_CHIP) : accent).orElse(FOREGROUND);
    }

    static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * alpha));
    }

    /** {@code a} moved {@code t} of the way towards {@code b}, opaque. */
    static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
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
