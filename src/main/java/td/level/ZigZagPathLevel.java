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
                            new WaveDefinition("c e c e c e c", Rank.GRUNT),
                            new WaveDefinition("5 c", Rank.GRUNT),
                            new WaveDefinition("s e s e s", Rank.SOLDIER),
                            new WaveDefinition("t e t e t e t", Rank.SOLDIER),
                            new WaveDefinition("3 s 2 e 3 c", Rank.SOLDIER),
                            new WaveDefinition("g e g e g", Rank.VETERAN),
                            new WaveDefinition("2 g 2 t 2 s 2 c", Rank.VETERAN),
                            new WaveDefinition("10 c e 5 s e 3 t e g", Rank.ELITE)),
                    SMOOTHING)
            .withColor(FUCHSIA_COLOR);

    private static final PathColor LIME_COLOR = PathColor.of(170, 230, 40);
    private static final PathDefinition LIME_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(12, -1), new Point(12, 2), new Point(9, 10), new Point(7, 10),
                            new Point(3, 2), new Point(3, -1)),
                    List.of(
                            new WaveDefinition("c e c e c", Rank.GRUNT),
                            new WaveDefinition("5 c", Rank.GRUNT),
                            new WaveDefinition("s e s e s", Rank.SOLDIER),
                            new WaveDefinition("t e t e t", Rank.SOLDIER),
                            new WaveDefinition("3 s 2 e 3 c", Rank.SOLDIER),
                            new WaveDefinition("g e g e g", Rank.VETERAN),
                            new WaveDefinition("2 g 2 t 2 s", Rank.VETERAN),
                            new WaveDefinition("10 c e 5 s e g", Rank.ELITE)),
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
