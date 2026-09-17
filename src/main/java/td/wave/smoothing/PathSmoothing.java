package td.wave.smoothing;

import td.wave.Vec2;

import java.util.List;

/**
 * A pluggable strategy for turning a straight-line, cell-center polyline into whatever
 * geometry a level actually wants enemies to walk and buildability to be computed from.
 */
public interface PathSmoothing {

    /**
     * The identity/no-op strategy - the polyline is returned exactly as given.
     */
    static PathSmoothing none() {
        return List::copyOf;
    }

    List<Vec2> smooth(List<Vec2> pixelPolyline);
}
