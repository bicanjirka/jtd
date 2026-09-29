package td.ui.render;

/**
 * A small marker for one active status effect on a mob. {@code hiddenCount} is how many further
 * effects an overflow marker stands for; {@code 0} for an ordinary marker.
 */
public record StatusMarkerDraw(Palette palette, float x, float y, float scale, int hiddenCount) {

    public StatusMarkerDraw(Palette palette, float x, float y, float scale) {
        this(palette, x, y, scale, 0);
    }
}
