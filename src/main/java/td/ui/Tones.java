package td.ui;

import java.awt.Color;

/**
 * One colour as the board's light sees it: the body, the side turned from the light and the rim turned to it. The light
 * comes from the top left for everything that stands on the board.
 */
record Tones(Color shade, Color body, Color highlight) {

    private static final float SHADE = 0.45f;
    private static final float HIGHLIGHT = 0.38f;

    static Tones of(Color body) {
        return new Tones(toward(body, Color.BLACK, SHADE), body, toward(body, Color.WHITE, HIGHLIGHT));
    }

    static Color toward(Color from, Color to, float share) {
        return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * share),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * share),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * share));
    }
}
