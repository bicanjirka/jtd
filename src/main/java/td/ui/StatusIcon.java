package td.ui;

import td.economy.StatusTerm;
import td.ui.render.Palette;
import td.util.ThreadConfined;

import javax.swing.JComponent;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.OptionalInt;

/**
 * One status bar entry: the term's framed face, then what it counts. The round is a bar of the level's rounds, the
 * lives a heart each (a number when there are too many or none), the score and the cash a bold number. The pointer
 * over it brightens it, and a flash brightens the face.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class StatusIcon extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Font VALUE_FONT = Hud.LABEL_FONT.deriveFont(Font.BOLD, 13f);
    private static final String WIDEST_NUMBER = "0000000";
    private static final Color BAR_BACKGROUND = new Color(12, 22, 12);
    private static final int PADDING = 4;
    private static final int GAP = 6;
    private static final int BAR_WIDTH = 64;
    private static final int BAR_HEIGHT = 8;
    private static final int HEART_SIZE = 12;
    private static final int HEART_GAP = 2;
    private static final int HOVER_ALPHA = 36;
    private static final int FLASH_ALPHA = 150;
    private static final int ARC = 6;

    private final StatusTerm term;
    private StatusReadout readout = StatusReadout.empty();
    private float flash = 0f;
    private boolean hovered = false;

    StatusIcon(StatusTerm term) {
        this.term = term;
        this.setOpaque(false);
        this.setFocusable(false);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                StatusIcon.this.hovered = true;
                StatusIcon.this.repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                StatusIcon.this.hovered = false;
                StatusIcon.this.repaint();
            }
        });
    }

    /** As wide as what it shows now needs, and never narrower than a number: the lives grow with their hearts. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(PADDING * 2 + TermIconPainter.ROW + GAP + this.contentWidth(),
                TermIconPainter.ROW + PADDING * 2);
    }

    private int contentWidth() {
        int number = this.getFontMetrics(VALUE_FONT).stringWidth(WIDEST_NUMBER);
        return switch (this.term) {
            case WAVE -> BAR_WIDTH;
            case LIVES -> Math.max(number, this.readout.hearts().orElse(0) * (HEART_SIZE + HEART_GAP) - HEART_GAP);
            case SCORE, CASH -> number;
        };
    }

    StatusTerm term() {
        return this.term;
    }

    StatusReadout readout() {
        return this.readout;
    }

    float flash() {
        return this.flash;
    }

    void show(StatusReadout next) {
        if (!next.equals(this.readout)) {
            int before = this.contentWidth();
            this.readout = next;
            if (this.contentWidth() != before) {
                this.revalidate();
            }
            this.repaint();
        }
    }

    /** How brightly the face flashes now, from 0 to 1. */
    void flash(float strength) {
        if (strength != this.flash) {
            this.flash = strength;
            this.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (this.hovered) {
            g2.setColor(new Color(255, 255, 255, HOVER_ALPHA));
            g2.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), ARC, ARC);
        }
        int top = (this.getHeight() - TermIconPainter.ROW) / 2;
        Graphics2D face = (Graphics2D) g2.create();
        face.translate(PADDING, top);
        TermIconPainter.paint(face, TermLooks.faceOf(this.term), TermIconPainter.ROW);
        face.dispose();
        if (this.flash > 0f) {
            g2.setColor(new Color(255, 255, 255, Math.round(FLASH_ALPHA * this.flash)));
            g2.fillRoundRect(PADDING, top, TermIconPainter.ROW, TermIconPainter.ROW, ARC, ARC);
        }
        int x = PADDING + TermIconPainter.ROW + GAP;
        switch (this.term) {
            case WAVE -> this.paintBar(g2, x);
            case LIVES -> this.paintLives(g2, x);
            case SCORE, CASH -> this.paintNumber(g2, x);
        }
        g2.dispose();
    }

    private void paintBar(Graphics2D g2, int x) {
        int y = (this.getHeight() - BAR_HEIGHT) / 2;
        g2.setColor(BAR_BACKGROUND);
        g2.fillRect(x, y, BAR_WIDTH, BAR_HEIGHT);
        g2.setColor(Java2DFrameRenderer.colorFor(Palette.TERM_STATUS_WAVE));
        g2.fillRect(x, y, Math.round(BAR_WIDTH * this.readout.waveShare()), BAR_HEIGHT);
        g2.setColor(Hud.BORDER_IDLE);
        g2.drawRect(x, y, BAR_WIDTH - 1, BAR_HEIGHT - 1);
    }

    private void paintLives(Graphics2D g2, int x) {
        OptionalInt hearts = this.readout.hearts();
        if (hearts.isEmpty()) {
            this.paintNumber(g2, x);
            return;
        }
        Pictogram heart = PictogramCatalogue.of(this.term.key()).orElseThrow();
        g2.setColor(Java2DFrameRenderer.colorFor(Palette.TERM_STATUS_LIVES));
        double y = (this.getHeight() - HEART_SIZE) / 2.0;
        double scale = HEART_SIZE / (double) Pictogram.UNIT;
        for (int i = 0; i < hearts.getAsInt(); i++) {
            Graphics2D one = (Graphics2D) g2.create();
            one.translate(x + i * (HEART_SIZE + HEART_GAP), y);
            one.scale(scale, scale);
            heart.layers().forEach(layer -> one.fill(layer.shape()));
            one.dispose();
        }
    }

    private void paintNumber(Graphics2D g2, int x) {
        g2.setFont(VALUE_FONT);
        FontMetrics metrics = g2.getFontMetrics();
        g2.setColor(Hud.FOREGROUND);
        g2.drawString(this.readout.value(this.term), x,
                (this.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
    }
}
