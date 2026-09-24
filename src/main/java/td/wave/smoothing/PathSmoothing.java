package td.wave.smoothing;

import td.wave.Vec2;

import java.util.List;

/** Turns a cell-centre polyline into the geometry enemies walk and buildability uses. */
public interface PathSmoothing {

    /** Returns the polyline unchanged. */
    static PathSmoothing none() {
        return List::copyOf;
    }

    List<Vec2> smooth(List<Vec2> pixelPolyline);
}
