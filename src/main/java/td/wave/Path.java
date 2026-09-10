package td.wave;

import java.util.List;

/**
 * The route enemies walk, as a polyline of continuous pixel-space points - already smoothed,
 * if the level asked for smoothing. Built only by {@link PathBuilder}; measured and sampled
 * only through {@link ArcLengthPath}.
 * <p>
 * A path with fewer than two points is a legitimate state (a world before any level loads)
 * that consumers must handle, not an error to guard against at construction.
 */
public interface Path {

    List<Vec2> points();

}
