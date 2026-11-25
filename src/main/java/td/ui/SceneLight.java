package td.ui;

import td.board.BoardLight;

import java.awt.Color;

/**
 * A level's light as the painter applies it to what stands on the board. A thing's top face keeps its exact colour,
 * so the board, the toolbar and the Info pane agree; its shaded side leans toward the level's shadow colour and its
 * rim toward the key light. {@link #none()} leaves tones as they are: a panel's picture is unlit.
 */
record SceneLight(Color key, Color shadow, float keyShare, float shadowShare) {

    /** How far a rim leans toward the key light. */
    private static final float KEY_SHARE = 0.3f;
    /** How far a shaded side leans toward the shadow colour; the shadow colour is dark, so this also deepens it. */
    private static final float SHADOW_SHARE = 0.4f;

    private static final SceneLight NONE = new SceneLight(Color.WHITE, Color.BLACK, 0f, 0f);

    static SceneLight none() {
        return NONE;
    }

    static SceneLight of(BoardLight light) {
        return new SceneLight(new Color(light.keyRgb()), new Color(light.shadowRgb()), KEY_SHARE, SHADOW_SHARE);
    }

    /** A shadow cast on the ground under this light: its shadow colour at {@code alpha} of 255. */
    Color shadowOf(int alpha) {
        return new Color(this.shadow.getRed(), this.shadow.getGreen(), this.shadow.getBlue(), alpha);
    }

    /** {@code tones} under this light: the body as it is, the shade and the rim leant toward the light's colours. */
    Tones on(Tones tones) {
        if (this.keyShare == 0f && this.shadowShare == 0f) {
            return tones;
        }
        return new Tones(Tones.toward(tones.shade(), this.shadow, this.shadowShare), tones.body(),
                Tones.toward(tones.highlight(), this.key, this.keyShare));
    }
}
