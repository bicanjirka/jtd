package td.wave;

import td.wave.smoothing.PathSmoothing;

import java.util.List;

/**
 * One lane's authored corners, smoothing, waves, colour and pace. Colour and pace default to white
 * and {@code 1x}; set them with {@link #withColor} and {@link #withSpeed}.
 */
public record PathDefinition(List<Point> corners, PathSmoothing smoothing, List<WaveDefinition> waves,
                              PathColor color, float speedMultiplier) {

    public PathDefinition {
        corners = List.copyOf(corners);
        waves = List.copyOf(waves);
        if (corners.size() < 2) {
            throw new IllegalArgumentException("A path needs at least 2 corners, had " + corners.size());
        }
        if (speedMultiplier <= 0f) {
            throw new IllegalArgumentException("A path's speed multiplier must be positive, was " + speedMultiplier);
        }
    }

    /** An unsmoothed path. */
    public static PathDefinition of(List<Point> corners, List<WaveDefinition> waves) {
        return smoothed(corners, waves, PathSmoothing.none());
    }

    public static PathDefinition smoothed(List<Point> corners, List<WaveDefinition> waves, PathSmoothing smoothing) {
        return new PathDefinition(corners, smoothing, waves, PathColor.DEFAULT, 1f);
    }

    public PathDefinition withColor(PathColor color) {
        return new PathDefinition(this.corners, this.smoothing, this.waves, color, this.speedMultiplier);
    }

    /** Multiplies with each wave's own {@link WaveDefinition#speedMultiplier()}. */
    public PathDefinition withSpeed(float speedMultiplier) {
        return new PathDefinition(this.corners, this.smoothing, this.waves, this.color, speedMultiplier);
    }
}
