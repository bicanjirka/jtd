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
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.Serial;
import java.util.OptionalInt;

/**
 * One status bar entry: the term's framed face, then what it counts. The round is a skull per round, as many as fit,
 * then how many more follow, the current one pulsing and a skull cracking as its round ends; the lives a heart each (a number when there are too many or none), the score and the cash a bold number. The pointer
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
    private static final long PULSE_PERIOD_MS = 1400;
    private static final float PULSE_LIGHTEN = 0.4f;
    private static final float CRACK_SHAKE_PX = 1.5f;
    private static final float CRACK_SHAKES = 3f;
    private static final float CRACK_POP = 0.3f;
    private static final float GLOW_RADIUS = 0.85f;
    private static final int GLOW_ALPHA = 80;
    private static final int HEART_SIZE = 12;
    private static final int HEART_GAP = 2;
    private static final int HOVER_ALPHA = 36;
    private static final int FLASH_ALPHA = 150;
    private static final int ARC = 6;

    private final StatusTerm term;
    private StatusReadout readout = StatusReadout.empty();
    private final RoundCracks cracks = new RoundCracks();
    private RoundStrip rounds = RoundStrip.empty();
    private long nowMillis = 0L;
    private boolean animating = false;
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

    /** The skull of round {@code round} cracking now: from 0 as it cracks to 1 once settled. */
    float crack(int round) {
        return this.cracks.progress(round, this.nowMillis);
    }

    /** The next strip is a baseline, cracking nothing. */
    void resetRounds() {
        this.cracks.reset();
    }

    void showRounds(RoundStrip next, long nowMillis) {
        if (!next.equals(this.rounds)) {
            int before = this.contentWidth();
            this.cracks.observe(next, nowMillis);
            this.nowMillis = nowMillis;
            this.rounds = next;
            if (this.contentWidth() != before) {
                this.revalidate();
            }
            this.repaint();
        }
    }

    /** Moves the strip's clock on: repaints while a skull pulses or cracks, and once more as it settles. */
    void animate(long nowMillis) {
        this.nowMillis = nowMillis;
        boolean moving = this.cracks.active(nowMillis) || this.rounds.states().contains(RoundStrip.State.CURRENT);
        if (moving || this.animating) {
            this.repaint();
        }
        this.animating = moving;
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
        float pulse = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * (this.nowMillis % PULSE_PERIOD_MS) / PULSE_PERIOD_MS));
        for (int round = window.start(); round < window.end(); round++) {
            RoundStrip.State state = this.rounds.stateAt(round);
            this.paintSkull(g2, round, left, y, pulse);
            if (state == RoundStrip.State.CURRENT) {
                Color mark = Java2DFrameRenderer.colorFor(Palette.ROUND_CURRENT);
                g2.setColor(new Color(mark.getRed(), mark.getGreen(), mark.getBlue(), Math.round(110 + 145 * pulse)));
                g2.fillRoundRect(left + 1, y + SKULL_SIZE + 2, SKULL_SIZE - 2, MARK_HEIGHT, MARK_HEIGHT, MARK_HEIGHT);
            }
            left += SKULL_SIZE + SKULL_GAP;
        }
        if (window.hiddenAfter() > 0) {
            g2.setFont(MORE_FONT);
            FontMetrics metrics = g2.getFontMetrics();
            g2.setColor(Java2DFrameRenderer.colorFor(RoundSkulls.kindTone(this.rounds.strongestFrom(window.end()))));
            g2.drawString("+" + window.hiddenAfter(), left - SKULL_GAP + MORE_GAP,
                    (this.getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
        }
    }

    /** One skull: dimmed to come, brightening and dimming while current, and on cracking a white flash that shakes and settles. */
    private void paintSkull(Graphics2D g2, int round, int x, int y, float pulse) {
        RoundStrip.State state = this.rounds.stateAt(round);
        RoundSkulls.Kind kind = this.rounds.kindAt(round);
        Pictogram skull = PictogramCatalogue.of(RoundSkulls.pictogram(kind, state)).orElseThrow();
        Color ink = Java2DFrameRenderer.colorFor(RoundSkulls.tone(kind, state));
        float crack = this.cracks.progress(round, this.nowMillis);
        float centre = SKULL_SIZE / 2f;
        if (kind == RoundSkulls.Kind.UNIQUE && !state.isOver()) {
            paintGlow(g2, ink, x + centre, y + centre, state == RoundStrip.State.CURRENT ? pulse : 0.5f, SKULL_SIZE);
        }
        switch (state) {
            case AHEAD -> ink = mix(ink, Color.BLACK, AHEAD_DARKEN);
            case CURRENT -> ink = mix(ink, Color.WHITE, PULSE_LIGHTEN * pulse);
            case CLEARED, LEAKED -> ink = mix(Color.WHITE, ink, crack);
        }
        float settle = 1f - crack;
        double shake = Math.sin(crack * Math.PI * 2 * CRACK_SHAKES) * settle * CRACK_SHAKE_PX;
        double pop = 1 + CRACK_POP * settle * settle;
        Graphics2D one = (Graphics2D) g2.create();
        one.translate(x + centre + shake, y + centre);
        double scale = pop * SKULL_SIZE / Pictogram.UNIT;
        one.scale(scale, scale);
        one.translate(-Pictogram.UNIT / 2, -Pictogram.UNIT / 2);
        // The strip sits on the header's black, so a knock-out is black too.
        TermIconPainter.paintLayers(one, skull, ink, Color.BLACK);
        one.dispose();
    }

    /** A round's skull held still in its own tone, for a panel that names the same round as the strip. */
    static void paintStillSkull(Graphics2D g2, RoundSkulls.Kind kind, RoundStrip.State state, float x, float y, int size) {
        Pictogram skull = PictogramCatalogue.of(RoundSkulls.pictogram(kind, state)).orElseThrow();
        Color ink = Java2DFrameRenderer.colorFor(RoundSkulls.tone(kind, state));
        float centre = size / 2f;
        if (kind == RoundSkulls.Kind.UNIQUE && !state.isOver()) {
            paintGlow(g2, ink, x + centre, y + centre, 0.5f, size);
        }
        Graphics2D one = (Graphics2D) g2.create();
        one.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        one.translate(x, y);
        double scale = (double) size / Pictogram.UNIT;
        one.scale(scale, scale);
        TermIconPainter.paintLayers(one, skull, ink, Hud.BACKGROUND);
        one.dispose();
    }

    private static void paintGlow(Graphics2D g2, Color tone, float cx, float cy, float strength, int size) {
        float radius = size * GLOW_RADIUS;
        Color inner = new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), Math.round(GLOW_ALPHA * (0.5f + strength)));
        Color outer = new Color(tone.getRed(), tone.getGreen(), tone.getBlue(), 0);
        Graphics2D glow = (Graphics2D) g2.create();
        glow.setPaint(new RadialGradientPaint(cx, cy, radius, new float[]{0f, 1f}, new Color[]{inner, outer}));
        glow.fill(new Ellipse2D.Float(cx - radius, cy - radius, radius * 2, radius * 2));
        glow.dispose();
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
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
