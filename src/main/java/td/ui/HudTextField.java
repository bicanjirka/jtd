package td.ui;

import td.util.ThreadConfined;

import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.BadLocationException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.io.Serial;

/**
 * A text input in the HUD look. Swing's cross-platform basic delegate paints it, never the
 * platform look-and-feel, so it looks the same on every OS.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class HudTextField extends JTextField {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color FIELD_BACKGROUND = new Color(29, 29, 30);
    private static final Color SELECTION = Hud.FILL_PRESSED;
    private static final Color GHOST = new Color(128, 128, 131);

    private String ghost = "";
    private String chip = "";

    HudTextField(String text, int columns) {
        super(text, columns);
        setUI(new BasicTextFieldUI());
        setFont(Hud.LABEL_FONT);
        setBackground(FIELD_BACKGROUND);
        setForeground(Hud.FOREGROUND);
        setCaretColor(Hud.FOREGROUND);
        setSelectionColor(SELECTION);
        setSelectedTextColor(Hud.FOREGROUND);
        setBorder(Hud.controlBorder(2, 4));
        setOpaque(false);
    }

    /** Muted text drawn after the caret, which Tab can accept; empty for none. */
    void setGhostText(String ghost) {
        if (!ghost.equals(this.ghost)) {
            this.ghost = ghost;
            repaint();
        }
    }

    /** A key chip over the field's corner for a moment, as the dev panel shows while Alt is held; "" for none. */
    void showChip(String label) {
        if (!label.equals(this.chip)) {
            this.chip = label;
            repaint();
        }
    }

    /** Its fill inside the rounded border, which an opaque field would square off. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D fill = (Graphics2D) g.create();
        try {
            fill.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            fill.setColor(getBackground());
            fill.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), Hud.ARC, Hud.ARC));
        } finally {
            fill.dispose();
        }
        super.paintComponent(g);
        if (!this.chip.isEmpty()) {
            Hud.paintChip((Graphics2D) g, this.chip, 1, 1, Hud.CHIP_BORDER, Hud.FOREGROUND);
        }
        if (this.ghost.isEmpty()) {
            return;
        }
        try {
            Rectangle2D caret = modelToView2D(getCaretPosition());
            g.setFont(getFont());
            g.setColor(GHOST);
            g.drawString(this.ghost, (int) caret.getX() + 1, (int) caret.getY() + g.getFontMetrics().getAscent());
        } catch (BadLocationException e) {
            throw new IllegalStateException("The caret is outside the text", e);
        }
    }
}
