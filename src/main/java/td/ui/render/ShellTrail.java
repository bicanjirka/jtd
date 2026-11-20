package td.ui.render;

import java.util.List;

/**
 * The tail a shell draws behind it along its arc: {@code points} on screen from its head back, {@code width} its
 * radius at the head in pixels, and {@code seed} the tick it is drawn at, so it flickers.
 */
public record ShellTrail(TrailLook look, float width, List<Point> points, int seed) {

    /** One point of the tail on screen, already lifted along the arc. */
    public record Point(float x, float y) {
    }
}
