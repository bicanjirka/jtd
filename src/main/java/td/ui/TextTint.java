package td.ui;

import java.awt.Color;

/**
 * The colour small text takes when it wears a lane's colour: the lane's own where that reads on the HUD background, or
 * the nearest lighter tint of it that does. The lane colour itself stays for borders and shapes.
 */
final class TextTint {

    private static final double SMALL_TEXT_CONTRAST = 7.0;
    private static final float LIGHTEN_STEP = 0.01f;

    private TextTint() {
    }

    static Color of(Color lane) {
        Color tint = lane;
        for (float toWhite = LIGHTEN_STEP; contrastOnBackground(tint) < SMALL_TEXT_CONTRAST && toWhite < 1f;
                toWhite += LIGHTEN_STEP) {
            tint = Hud.mix(lane, Color.WHITE, toWhite);
        }
        return tint;
    }

    /** The WCAG contrast ratio of {@code color} on the HUD background. */
    static double contrastOnBackground(Color color) {
        return (luminance(color) + 0.05) / (luminance(Hud.BACKGROUND) + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * linear(color.getRed()) + 0.7152 * linear(color.getGreen()) + 0.0722 * linear(color.getBlue());
    }

    private static double linear(int channel) {
        double share = channel / 255.0;
        return share <= 0.04045 ? share / 12.92 : Math.pow((share + 0.055) / 1.055, 2.4);
    }
}
