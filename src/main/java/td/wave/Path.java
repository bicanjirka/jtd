package td.wave;

import java.util.List;

/**
 * The route enemies walk: pixel-space points, already smoothed, held as an immutable copy. Built by
 * {@link PathBuilder}, measured through {@link ArcLengthPath}. Fewer than two points is a valid state that
 * consumers must handle.
 */
public record Path(List<Vec2> points) {

    public Path {
        points = List.copyOf(points);
    }
}
