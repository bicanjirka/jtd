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
 * One status bar entry: the term's framed face, then what it counts. The round is a skull per round, as many as fit,
 * then how many more follow; the lives a heart each (a number when there are too many or none), the score and the cash a bold number. The pointer
 * over it brightens it, and a flash brightens the face.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class StatusIcon extends JComponent {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Font VALUE_FONT = Hud.LABEL_FONT.deriveFont(Font.BOLD, 13f);
    private static final String WIDEST_NUMBER = "0000000";
    private static final Font MORE_FONT = Hud.LABEL_FONT.deriveFont(Font.BOLD, 11f);
    private static final String WIDEST_MORE = "+99";
    private static final int PADDING = 4;
    private static final int GAP = 6;
    private static final int MAX_SKULLS = 10;
    private static final int SKULL_SIZE = 14;
    private static final int SKULL_GAP = 2;
    private static final int MORE_GAP = 4;
    private static final int MARK_HEIGHT = 2;
    private static final float AHEAD_DARKEN = 0.3f;
    private static final int HEART_SIZE = 12;
    private static final int HEART_GAP = 2;
    private static final int HOVER_ALPHA = 36;
    private static final int FLASH_ALPHA = 150;
    private static final int ARC = 6;

    private final StatusTerm term;
    private StatusReadout readout = StatusReadout.empty();
    private RoundStrip rounds = RoundStrip.empty();
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
            case WAVE -> this.roundsWidth();
            case LIVES -> Math.max(number, this.readout.hearts().orElse(0) * (HEART_SIZE + HEART_GAP) - HEART_GAP);
            case SCORE, CASH -> number;
        };
    }

    private int roundsWidth() {
        RoundStrip.Window window = this.rounds.window(MAX_SKULLS);
        int shown = window.end() - window.start();
        int skulls = Math.max(0, shown * (SKULL_SIZE + SKULL_GAP) - SKULL_GAP);
        return window.hiddenAfter() > 0
                ? skulls + MORE_GAP + this.getFontMetrics(MORE_FONT).stringWidth(WIDEST_MORE)
                : skulls;
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

    RoundStrip rounds() {
        return this.rounds;
    }

    void showRounds(RoundStrip next) {
        if (!next.equals(this.rounds)) {
            int before = this.contentWidth();
            this.rounds = next;
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
            case WAVE -> this.paintRounds(g2, x);
            case LIVES -> this.paintLives(g2, x);
            case SCORE, CASH -> this.paintNumber(g2, x);
        }
        g2.dispose();
    }

    private void paintRounds(Graphics2D g2, int x) {
        RoundStrip.Window window = this.rounds.window(MAX_SKULLS);
        int y = (this.getHeight() - SKULL_SIZE - MARK_HEIGHT) / 2 - 1;
        int left = x;
        for (int round = window.start(); round < window.end(); round++) {
            RoundStrip.State state = this.rounds.stateAt(round);
            this.paintSkull(g2, round, left, y);
            if (state == RoundStrip.State.CURRENT) {
                g2.setColor(Java2DFrameRenderer.colorFor(Palette.ROUND_CURRENT));
                g2.fillRoundRect(left + 1, y + SKULL_SIZE + 2, SKULL_SIZE - 2, MARK_HEIGHT, MARK_HEIGHT, MARK_HEIGHT);
            }
            left += SKULL_SIZE + SKULL_GAP;
        }
        if (window.hiddenAfter() > 0) {
            g2.setFont(MORE_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            g2.setColor(Java2DFrameRenderer.colorFor(RoundSkulls.rankTone(this.rounds.highestRankFrom(window.end()))));
            g2.drawString("+" + window.hiddenAfter(), left - SKULL_GAP + MORE_GAP,
                    (this.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        }
    }

    private void paintSkull(Graphics2D g2, int round, int x, int y) {
        RoundStrip.State state = this.rounds.stateAt(round);
        Pictogram skull = PictogramCatalogue.of(RoundSkulls.pictogram(this.rounds.rankAt(round), state)).orElseThrow();
        Color ink = Java2DFrameRenderer.colorFor(RoundSkulls.tone(this.rounds.rankAt(round), state));
        if (state == RoundStrip.State.AHEAD) {
            ink = new Color(Math.round(ink.getRed() * (1 - AHEAD_DARKEN)), Math.round(ink.getGreen() * (1 - AHEAD_DARKEN)),
                    Math.round(ink.getBlue() * (1 - AHEAD_DARKEN)));
        }
        Graphics2D one = (Graphics2D) g2.create();
        one.translate(x, y);
        double scale = SKULL_SIZE / (double) Pictogram.UNIT;
        one.scale(scale, scale);
        // The strip sits on the header's black, so a knock-out is black too.
        TermIconPainter.paintLayers(one, skull, ink, Color.BLACK);
        one.dispose();
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
