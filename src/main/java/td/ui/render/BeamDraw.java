package td.ui.render;

/**
 * A line from a tower to its target, or from a splash centre to a mob it caught.
 * {@code strokeWidth} thins as the tower recharges; {@code alpha} (0..1) fades the palette colour.
 */
public record BeamDraw(Palette palette, float fromX, float fromY, float toX, float toY,
                       float strokeWidth, float alpha) implements TowerEffectDraw {

    /** A beam at the palette's own opacity. */
    public static BeamDraw solid(Palette palette, float fromX, float fromY, float toX, float toY,
            float strokeWidth) {
        return new BeamDraw(palette, fromX, fromY, toX, toY, strokeWidth, 1f);
    }
}
