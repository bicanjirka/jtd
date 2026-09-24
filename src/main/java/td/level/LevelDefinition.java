package td.level;

import td.enemy.EnemyDefinition;
import td.enemy.RankedEnemy;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.PathSmoothing;

import java.util.List;

/**
 * A playable level: name, board size, one or more {@link PathDefinition}s and starting economy.
 * <p>
 * <strong>Every path must define the same number of waves.</strong> Waves run as synchronized
 * rounds: round {@code N} spawns every path's wave {@code N}, and clears once all of them are gone.
 * <p>
 * {@code customEnemies} and {@code customRankedEnemies} are registered into the level's catalog
 * after the built-ins and before any wave is parsed, so wave scripts name them like built-ins.
 */
public record LevelDefinition(
        String name,
        String description,
        int width,
        int height,
        List<PathDefinition> paths,
        List<EnemyDefinition> customEnemies,
        List<RankedEnemy> customRankedEnemies,
        int startingCredits,
        int startingLives) {

    public LevelDefinition {
        paths = List.copyOf(paths);
        customEnemies = List.copyOf(customEnemies);
        customRankedEnemies = List.copyOf(customRankedEnemies);
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
     * The required shape: name, board size and paths, with the default economy ($100, 5 lives), no
     * description and no custom enemies. Add the rest with the {@code withX} copies.
     */
    public static LevelDefinition of(String name, int width, int height, List<PathDefinition> paths) {
        return new LevelDefinition(name, "", width, height, paths, List.of(), List.of(), 100, 5);
    }

    public LevelDefinition withDescription(String description) {
        return new LevelDefinition(this.name, description, this.width, this.height, this.paths,
                this.customEnemies, this.customRankedEnemies, this.startingCredits, this.startingLives);
    }

    public LevelDefinition withStartingCredits(int startingCredits) {
        return new LevelDefinition(this.name, this.description, this.width, this.height, this.paths,
                this.customEnemies, this.customRankedEnemies, startingCredits, this.startingLives);
    }

    public LevelDefinition withStartingLives(int startingLives) {
        return new LevelDefinition(this.name, this.description, this.width, this.height, this.paths,
                this.customEnemies, this.customRankedEnemies, this.startingCredits, startingLives);
    }

    /** Single-rank enemies this level adds to its catalog. */
    public LevelDefinition withCustomEnemies(List<EnemyDefinition> customEnemies) {
        return new LevelDefinition(this.name, this.description, this.width, this.height, this.paths,
                customEnemies, this.customRankedEnemies, this.startingCredits, this.startingLives);
    }

    /** Ranked enemies this level adds to its catalog. */
    public LevelDefinition withCustomRankedEnemies(List<RankedEnemy> customRankedEnemies) {
        return new LevelDefinition(this.name, this.description, this.width, this.height, this.paths,
                this.customEnemies, customRankedEnemies, this.startingCredits, this.startingLives);
    }

    /** A level with one unsmoothed path. */
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

    /** A level with one path and the given smoothing. */
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
