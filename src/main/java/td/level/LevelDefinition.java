package td.level;

import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.PathSmoothing;

import java.util.List;

/**
 * A named, playable level: board size, the enemy path across it, its waves,
 * and its own starting economy. {@code path} holds the level's corners, in
 * authored order - as few as two, connected by straight legs of any length
 * and any angle. {@link td.wave.PathBuilder} converts each corner to its
 * pixel-space center and runs the result through {@code smoothing} before
 * enemies ever move along it - there is no separate per-cell expansion step,
 * and no requirement that consecutive corners be axis-aligned.
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
        if (path.size() < 2) {
            throw new IllegalArgumentException("A level's path needs at least 2 corners, had " + path.size());
        }
    }

    /**
     * A level with no path smoothing - the common case for a level that doesn't care.
     */
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
