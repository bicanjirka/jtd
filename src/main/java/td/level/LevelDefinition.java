package td.level;

import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;

/**
 * A named, playable level: board size, the enemy path across it, its waves,
 * and its own starting economy. {@code path} holds cell coordinates (one
 * entry per cell the path crosses, in order) - not the pixel coordinates
 * {@link td.wave.Path#getStep} deals in, despite sharing the {@link Point}
 * type with it.
 */
public record LevelDefinition(
        String name,
        String description,
        int width,
        int height,
        List<Point> path,
        List<WaveDefinition> waves,
        int startingCredits,
        int startingLives) {

    public LevelDefinition {
        path = List.copyOf(path);
        waves = List.copyOf(waves);
    }
}
