package td.level;

import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.PathSmoothing;

import java.util.List;

/**
 * A named, playable level: board size, the enemy path across it, its waves,
 * and its own starting economy. {@code path} holds cell coordinates (one
 * entry per cell the path crosses, in order) - not the pixel coordinates
 * {@link td.wave.Path#getStep} deals in, despite sharing the {@link Point}
 * type with it. {@code smoothing} is applied to that path's pixel-space
 * form (see {@link td.wave.PathBuilder}) before enemies ever move along it.
 */
public record LevelDefinition(
        String name,
        String description,
        int width,
        int height,
        List<Point> path,
        List<WaveDefinition> waves,
        int startingCredits,
        int startingLives,
        PathSmoothing smoothing) {

    public LevelDefinition {
        path = List.copyOf(path);
        waves = List.copyOf(waves);
    }

    /** A level with no path smoothing - the common case for a level that doesn't care. */
    public static LevelDefinition unsmoothed(
            String name,
            String description,
            int width,
            int height,
            List<Point> path,
            List<WaveDefinition> waves,
            int startingCredits,
            int startingLives) {
        return new LevelDefinition(name, description, width, height, path, waves,
                startingCredits, startingLives, PathSmoothing.none());
    }
}
