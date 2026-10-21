package td.ui;

import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;
import td.util.ThreadConfined;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.View;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/** One tooltip: a bordered {@link SheetPane} sized to its sheet, up to {@link #MAX_WIDTH}. */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class TooltipPanel extends JPanel {

    static final int MAX_WIDTH = 240;
    private static final Color BACKGROUND = new Color(8, 14, 8);
    private static final float EXPIRED_ALPHA = 0.45f;

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Java2DFrameRenderer renderer = new Java2DFrameRenderer();
    private final SheetPane pane = new SheetPane();
    private InfoSheet sheet = new InfoSheet(List.of());
    private boolean expired;

    TooltipPanel() {
        super(new BorderLayout());
        this.setBackground(BACKGROUND);
        this.setBorder(BorderFactory.createCompoundBorder(Hud.outlineBorder(), new EmptyBorder(4, 6, 4, 6)));
        this.pane.setBackground(BACKGROUND);
        this.add(this.pane, BorderLayout.CENTER);
    }

    SheetPane pane() {
        return this.pane;
    }

    /** The sheet it shows now, without the Expired note. */
    InfoSheet sheet() {
        return this.sheet;
    }

    /** Shows {@code next}; an identical sheet on a live tooltip is left alone. */
    void show(InfoSheet next) {
        if (next.equals(this.sheet) && !this.expired) {
            return;
        }
        this.sheet = next;
        this.expired = false;
        this.layOut(next);
    }

    /** Keeps what it showed and says it no longer applies. */
    void showExpired() {
        if (this.expired) {
            return;
        }
        this.expired = true;
        List<SheetLine> lines = new ArrayList<>(this.sheet.lines());
        lines.add(new SheetLine.Prose(TermText.of("Expired")));
        this.layOut(new InfoSheet(lines));
    }

    private void layOut(InfoSheet shown) {
        Insets insets = this.getInsets();
        int inner = MAX_WIDTH - insets.left - insets.right;
        this.pane.setSize(inner, 1);
        this.pane.show(shown, this.renderer, inner);
        View root = this.pane.getUI().getRootView(this.pane);
        int natural = (int) Math.ceil(root.getPreferredSpan(View.X_AXIS));
        int width = Math.min(inner, Math.max(natural, 1));
        if (width < inner) {
            this.pane.setSize(width, 1);
            this.pane.show(shown, this.renderer, width);
        }
        this.pane.setSize(width, 1);
        Dimension preferred = this.pane.getPreferredSize();
        this.pane.setPreferredSize(new Dimension(width, preferred.height));
        this.setSize(new Dimension(width + insets.left + insets.right, preferred.height + insets.top + insets.bottom));
        this.revalidate();
        this.repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (!this.expired) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, EXPIRED_ALPHA));
        super.paint(g2);
        g2.dispose();
    }
}
