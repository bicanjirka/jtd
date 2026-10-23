package td.level;

import td.enemy.Rank;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

final class ZigZagPathLevel {

    private static final int STARTING_CREDITS = 75;
    private static final int STARTING_LIVES = 3;
    private static final String LEVEL_NAME = "Zigzag Path";
    private static final String LEVEL_DESCRIPTION = "Two lanes weave through the same cramped arena - fuchsia loops back on itself "
            + "before lime cuts clean across it.";

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.4, 10);

    private static final PathColor FUCHSIA_COLOR = PathColor.of(230, 60, 200);
    private static final PathDefinition FUCHSIA_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 2), new Point(13, 2), new Point(13, 5), new Point(2, 5),
                            new Point(2, 8), new Point(16, 8)),
                    List.of(
                            new WaveDefinition("simple e simple e simple e simple e 4 simple e 3 armored", Rank.GRUNT),
                            new WaveDefinition("5 simple e 4 simple e 2 armored", Rank.GRUNT),
                            new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.SOLDIER),
                            new WaveDefinition("fast e fast e fast e fast e 4 simple e 2 armored e fast", Rank.SOLDIER),
                            new WaveDefinition("3 armored 2 e 3 simple 2 e 4 armored 2 e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("ghost e ghost e ghost e 3 armored e 3 simple e ghost", Rank.VETERAN),
                            new WaveDefinition("2 ghost 2 fast 2 armored 2 simple 2 e 2 ghost 2 fast 4 simple", Rank.VETERAN),
                            new WaveDefinition("10 simple e 5 armored e 3 fast e ghost e 8 simple e 4 armored e 2 ghost", Rank.ELITE)),
                    SMOOTHING)
            .withColor(FUCHSIA_COLOR);

    private static final PathColor LIME_COLOR = PathColor.of(170, 230, 40);
    private static final PathDefinition LIME_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(12, -1), new Point(12, 2), new Point(9, 10), new Point(7, 10),
                            new Point(3, 2), new Point(3, -1)),
                    List.of(
                            new WaveDefinition("simple e simple e simple e 4 simple e 3 armored", Rank.GRUNT),
                            new WaveDefinition("5 simple e 4 simple e 2 armored", Rank.GRUNT),
                            new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.SOLDIER),
                            new WaveDefinition("fast e fast e fast e 4 simple e 2 armored e fast", Rank.SOLDIER),
                            new WaveDefinition("3 armored 2 e 3 simple 2 e 4 armored 2 e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("ghost e ghost e ghost e 3 armored e 3 simple e ghost", Rank.VETERAN),
                            new WaveDefinition("2 ghost 2 fast 2 armored 2 e 2 ghost 2 fast 4 simple", Rank.VETERAN),
                            new WaveDefinition("10 simple e 5 armored e ghost e 8 simple e 4 armored e ghost", Rank.ELITE)),
                    SMOOTHING)
            .withColor(LIME_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 16, 11, List.of(FUCHSIA_PATH, LIME_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES);

    private ZigZagPathLevel() {
    }
}
