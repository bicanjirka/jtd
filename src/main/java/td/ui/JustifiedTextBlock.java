package td.ui;

import td.util.ThreadConfined;

import javax.swing.JComponent;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.io.Serial;
import java.util.List;

/**
 * Read-only text set flush with both edges, hyphenated where a word would leave a wide gap. Its height follows its
 * width, so it re-lays out its parent when a resize changes how the text wraps.
 */
@ThreadConfined(ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class JustifiedTextBlock extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String text;
    private final transient Hyphenator hyphenator;

    JustifiedTextBlock(String text, Hyphenator hyphenator) {
        this.text = text;
        this.hyphenator = hyphenator;
    }

    @Override
    public Dimension getPreferredSize() {
        if (this.isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = this.getInsets();
        FontMetrics metrics = this.getFontMetrics(this.getFont());
        int rows = this.lines(metrics).size();
        int width = this.getWidth() > 0 ? this.getWidth() : metrics.stringWidth(this.text) + insets.left + insets.right;
        return new Dimension(width, rows * metrics.getHeight() + insets.top + insets.bottom);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean rewraps = width != this.getWidth();
        super.setBounds(x, y, width, height);
        if (rewraps) {
            this.revalidate();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(this.getFont());
        g2.setColor(this.getForeground());
        FontMetrics metrics = g2.getFontMetrics();
        Insets insets = this.getInsets();
        int width = this.textWidth();
        int baseline = insets.top + metrics.getAscent();
        for (JustifiedLineBreaker.TextLine line : this.lines(metrics)) {
            this.paintLine(g2, metrics, line, insets.left, baseline, width);
            baseline += metrics.getHeight();
        }
        g2.dispose();
    }

    private void paintLine(Graphics2D g2, FontMetrics metrics, JustifiedLineBreaker.TextLine line, int left, int baseline,
            int width) {
        List<String> words = line.words();
        int gaps = words.size() - 1;
        int inked = words.stream().mapToInt(metrics::stringWidth).sum();
        int spare = line.stretched() && gaps > 0 ? width - inked : gaps * metrics.stringWidth(" ");
        int x = left;
        for (int i = 0; i < words.size(); i++) {
            g2.drawString(words.get(i), x, baseline);
            // Spreads the spare pixels so their rounding never piles up on one gap.
            x += metrics.stringWidth(words.get(i)) + (i < gaps ? spare * (i + 1) / gaps - spare * i / gaps : 0);
        }
    }

    private List<JustifiedLineBreaker.TextLine> lines(FontMetrics metrics) {
        int width = this.getWidth() > 0 ? this.textWidth() : Integer.MAX_VALUE;
        return new JustifiedLineBreaker(this.hyphenator, metrics::stringWidth).lines(this.text, width);
    }

    private int textWidth() {
        Insets insets = this.getInsets();
        return this.getWidth() - insets.left - insets.right;
    }
}
