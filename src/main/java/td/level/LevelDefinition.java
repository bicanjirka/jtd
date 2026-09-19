package td.level;

import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.PathSmoothing;

import java.util.List;

/**
 * A named, playable level: board size, its one or more enemy paths, and its own starting
 * economy. Each path is a {@link PathDefinition} - its own corners, smoothing, waves, color and
 * speed. {@link td.wave.PathBuilder} converts each path's corners to pixel-space centers and
 * runs the result through that path's own smoothing before enemies ever move along it.
 * <p>
 * <strong>Every path in a level must define the same number of waves.</strong> A level's waves
 * run as synchronized rounds: starting round {@code N} spawns every path's wave {@code N}
 * together, and the round is cleared only once every path's enemies from it are gone (see
 * {@code td/wave/CLAUDE.md}) - a level whose paths disagree on round count is an authoring
 * mistake caught here, not a runtime surprise.
 */
public record LevelDefinition(
        String name,
        String description,
        int width,
        int height,
        List<PathDefinition> paths,
        int startingCredits,
        int startingLives) {

    public LevelDefinition {
        paths = List.copyOf(paths);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("A level needs at least 1 path");
        }
        int roundCount = paths.getFirst().waves().size();
        for (PathDefinition path : paths) {
            if (path.waves().size() != roundCount) {
                throw new IllegalArgumentException(
                        "Every path in a level must define the same number of waves, had "
                                + roundCount + " and " + path.waves().size());
            }
        }
    }

    /**
     * The required shape every level has: its name, board size and paths, at the traditional
     * default starting economy ($100/5 lives) and no description. A level that needs any of
     * those calls the matching {@code withX} copy below instead of this factory growing another
     * parameter - the same "with"-copy shape {@link PathDefinition} and {@code td.util.LoadedLevel}
     * already use.
     */
    public static LevelDefinition of(String name, int width, int height, List<PathDefinition> paths) {
        return new LevelDefinition(name, "", width, height, paths, 100, 5);
    }

    public LevelDefinition withDescription(String description) {
        return new LevelDefinition(this.name, description, this.width, this.height, this.paths,
                this.startingCredits, this.startingLives);
    }

    public LevelDefinition withStartingCredits(int startingCredits) {
        return new LevelDefinition(this.name, this.description, this.width, this.height, this.paths,
                startingCredits, this.startingLives);
    }

    public LevelDefinition withStartingLives(int startingLives) {
        return new LevelDefinition(this.name, this.description, this.width, this.height, this.paths,
                this.startingCredits, startingLives);
    }

    /**
     * A level with one unsmoothed path - the common case for a level that doesn't care about
     * multiple paths or smoothing.
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
        return singlePath(name, description, width, height, path, waves, startingCredits, startingLives,
                PathSmoothing.none());
    }

    /**
     * A level with one path, given an explicit smoothing strategy.
     */
    public static LevelDefinition singlePath(
            String name,
            String description,
            int width,
            int height,
            List<Point> path,
            List<WaveDefinition> waves,
            int startingCredits,
            int startingLives,
            PathSmoothing smoothing) {
        return of(name, width, height, List.of(PathDefinition.smoothed(path, waves, smoothing)))
                .withDescription(description)
                .withStartingCredits(startingCredits)
                .withStartingLives(startingLives);
    }
}
