package td.ui;

import td.util.ThreadConfined;

import javax.swing.JComponent;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.Serial;

/**
 * One button row's piece of the bracket that joins an exclusive choice's buttons: a tick toward
 * a member's button, and the spine running down to the next member.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class ChoiceBracket extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int WIDTH = 9;
    private static final float STROKE_WIDTH = 1.5f;

    private Segment segment = Segment.NONE;

    ChoiceBracket() {
        setPreferredSize(new Dimension(WIDTH, 1));
        setMinimumSize(new Dimension(WIDTH, 1));
    }

    void setSegment(Segment segment) {
        if (this.segment != segment) {
            this.segment = segment;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (this.segment == Segment.NONE) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Hud.FOREGROUND);
        g2.setStroke(new BasicStroke(STROKE_WIDTH));
        int spine = getWidth() - 3;
        int middle = getHeight() / 2;
        int top = this.segment == Segment.FIRST ? middle : 0;
        int bottom = this.segment == Segment.LAST ? middle : getHeight();
        g2.drawLine(spine, top, spine, bottom);
        if (this.segment != Segment.PASS) {
            g2.drawLine(0, middle, spine, middle);
        }
        g2.dispose();
    }

    /** Where a button row sits in a choice: outside it, a member, or a non-member the spine passes. */
    enum Segment {
        NONE, FIRST, MIDDLE, LAST, PASS
    }
}
