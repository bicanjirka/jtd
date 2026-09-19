package td.wave;

import td.wave.smoothing.PathSmoothing;

import java.util.List;

/**
 * One lane's authored geometry, waves, color and pace - a {@link td.level.LevelDefinition} owns
 * a list of these instead of one path. {@link #of}/{@link #smoothed} are the required shape
 * (corners and waves); color and speed are optional and default to a single path's traditional
 * look (white, {@code 1x}) via the fluent {@link #withColor}/{@link #withSpeed} copies, the same
 * "with"-copy shape {@code td.util.LoadedLevel} already uses, rather than growing constructor
 * parameters for configuration that most paths never need.
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

    /**
     * A path with no smoothing - the common case.
     */
    public static PathDefinition of(List<Point> corners, List<WaveDefinition> waves) {
        return smoothed(corners, waves, PathSmoothing.none());
    }

    /**
     * A path with an explicit smoothing strategy.
     */
    public static PathDefinition smoothed(List<Point> corners, List<WaveDefinition> waves, PathSmoothing smoothing) {
        return new PathDefinition(corners, smoothing, waves, PathColor.DEFAULT, 1f);
    }

    /**
     * This path, drawn in a different color - defaults to {@link PathColor#DEFAULT} when never called.
     */
    public PathDefinition withColor(PathColor color) {
        return new PathDefinition(this.corners, this.smoothing, this.waves, color, this.speedMultiplier);
    }

    /**
     * This path, walked at a different pace - defaults to {@code 1x} when never called. Composes
     * multiplicatively with a wave's own {@link WaveDefinition#speedMultiplier()}.
     */
    public PathDefinition withSpeed(float speedMultiplier) {
        return new PathDefinition(this.corners, this.smoothing, this.waves, this.color, speedMultiplier);
    }
}
