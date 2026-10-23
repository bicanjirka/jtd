package td.ui;

import td.util.ThreadConfined;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.text.BadLocationException;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.io.Serial;

/**
 * A text input in the HUD look. Swing's cross-platform basic delegate paints it, never the
 * platform look-and-feel, so it looks the same on every OS.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class HudTextField extends JTextField {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Color FIELD_BACKGROUND = new Color(14, 22, 14);
    private static final Color SELECTION = new Color(46, 66, 46);
    private static final Color GHOST = new Color(100, 130, 100);

    private String ghost = "";

    HudTextField(String text, int columns) {
        super(text, columns);
        setUI(new BasicTextFieldUI());
        setFont(Hud.LABEL_FONT);
        setBackground(FIELD_BACKGROUND);
        setForeground(Hud.FOREGROUND);
        setCaretColor(Hud.FOREGROUND);
        setSelectionColor(SELECTION);
        setSelectedTextColor(Hud.FOREGROUND);
        setBorder(BorderFactory.createCompoundBorder(Hud.outlineBorder(), BorderFactory.createEmptyBorder(2, 4, 2, 4)));
    }

    /** Muted text drawn after the caret, which Tab can accept; empty for none. */
    void setGhostText(String ghost) {
        if (!ghost.equals(this.ghost)) {
            this.ghost = ghost;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
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
