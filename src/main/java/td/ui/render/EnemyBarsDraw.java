package td.ui.render;

import java.util.List;

/**
 * The bars under a live enemy, stacked downward on one track.
 *
 * @param x     the centre of the stack
 * @param y     the top of the first bar
 * @param width every bar's width when full
 * @param alpha {@code 1} in full view, fading with a cloaked body
 */
public record EnemyBarsDraw(float x, float y, float width, float alpha, List<Bar> bars) implements EnemyOverlayDraw {

    /** One bar: what it measures, and how full it is from {@code 0} to {@code 1}. */
    public record Bar(EnemyBarLook look, float fill) {
    }
}
