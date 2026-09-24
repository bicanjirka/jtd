package td.wave;

import java.util.List;

/**
 * The route enemies walk: pixel-space points, already smoothed. Built by {@link PathBuilder},
 * measured through {@link ArcLengthPath}. Fewer than two points is a valid state that consumers
 * must handle.
 */
public interface Path {

    List<Vec2> points();

}
