package td.ui;

import td.term.TermNumbers;
import td.util.ThreadConfined;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;

/**
 * The one look of whatever a click buys, an upgrade node and a tower tile alike: its size on screen, border, corner,
 * and the price tag on its bottom edge, in pixels.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class BuyTileLook {

    /** What a price tag says about its price. */
    enum Tone {
        /** Bought: a tick in the owner's colour. */
        OWNED,
        BUYABLE,
        SHORT_OF_CASH,
        /** Not on offer now: locked, or needing something first. */
        UNAVAILABLE
    }

    static final float SIZE = 40f;
    static final float BORDER = 2f;
    static final float BORDER_TAKEN = 3f;
    /** A square's corner, as an arc's width. */
    static final float CORNER = 8f;
    static final float TAG_HEIGHT = 14f;

    private static final Font TAG_FONT = Hud.BODY_FONT;
    private static final Font TAG_BOLD = TAG_FONT.deriveFont(Font.BOLD);
    private static final Color TAG_FILL = new Color(16, 16, 18);
    private static final Color TAG_BORDER = new Color(58, 58, 61);
    private static final Color GLOSS = new Color(141, 141, 144);
    private static final float TICK_WIDTH = 16f;
    private static final float TAG_PAD = 7f;
    private static final float TAG_ARC = 5f;
    private static final float BASELINE = 4.5f;

    private BuyTileLook() {
    }

    /** How far a tag hangs below the edge it sits on. */
    static int tagOverhang() {
        return Math.round(TAG_HEIGHT / 2);
    }

    /**
     * The tag centred at {@code x} on the edge at {@code y}: the price, or a tick in {@code accent} once owned.
     */
    static void paintTag(Graphics2D g, float x, float y, int price, Tone tone, Color accent) {
        boolean owned = tone == Tone.OWNED;
        String text = owned ? "✓" : TermNumbers.cash(price);
        Font font = owned || tone == Tone.BUYABLE ? TAG_BOLD : TAG_FONT;
        g.setFont(font);
        float width = owned ? TICK_WIDTH : g.getFontMetrics().stringWidth(text) + TAG_PAD;
        Shape tag = new RoundRectangle2D.Float(x - width / 2, y - TAG_HEIGHT / 2, width, TAG_HEIGHT, TAG_ARC, TAG_ARC);
        g.setColor(TAG_FILL);
        g.fill(tag);
        g.setColor(owned ? accent : TAG_BORDER);
        g.setStroke(new BasicStroke(0.8f));
        g.draw(tag);
        g.setColor(switch (tone) {
            case OWNED -> accent;
            case BUYABLE -> Hud.FOREGROUND;
            case SHORT_OF_CASH -> Hud.SHORT;
            case UNAVAILABLE -> GLOSS;
        });
        g.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2f, y + BASELINE);
    }
}
