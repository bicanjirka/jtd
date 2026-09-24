package td.level;

import td.enemy.Rank;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

final class CurlyPathLevel {

    private static final int STARTING_CREDITS = 50;
    private static final int STARTING_LIVES = 5;

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.22, 8);

    static final LevelDefinition DEFINITION = LevelDefinition.singlePath(
            "Curly Path",
            "A lane that spirals through two tight loops before unwinding into a steady zigzag, creating a curly visuals.",
            20, 13,
            List.of(
                    new Point(-1, 11), new Point(2, 11), new Point(2, 2), new Point(6, 2),
                    new Point(6, 8), new Point(3, 8), new Point(3, 5), new Point(7, 5),
                    new Point(7, 10), new Point(10, 10), new Point(10, 4), new Point(8, 4),
                    new Point(8, 7), new Point(13, 7), new Point(13, 2), new Point(16, 2),
                    new Point(16, 11), new Point(18, 11), new Point(18, 7), new Point(20, 7)),
            List.of(
                    new WaveDefinition("4 c", Rank.GRUNT),
                    new WaveDefinition("9 c e s", Rank.GRUNT),
                    new WaveDefinition("c e c", Rank.SOLDIER),
                    new WaveDefinition("4 c 2 e 2 s", Rank.GRUNT),
                    new WaveDefinition("c c e s", Rank.VETERAN),
                    new WaveDefinition("10 c", Rank.GRUNT),
                    new WaveDefinition("3 s e 4 c t e s t", Rank.SOLDIER),
                    new WaveDefinition("2 c e e t", Rank.ELITE),
                    new WaveDefinition("g 2 e 2 s", Rank.SOLDIER),
                    new WaveDefinition("s t s c g c t c s g t c s g c t s g t c", Rank.SOLDIER),
                    new WaveDefinition("g c g", Rank.ELITE),
                    new WaveDefinition("6 g 2 e 4 t 2 m", Rank.VETERAN),
                    new WaveDefinition("c e c e c e c e c", Rank.SOLDIER),
                    new WaveDefinition("2 s 3 t 2 g 4 e c", Rank.VETERAN),
                    new WaveDefinition("s 4 e t", Rank.BOSS),
                    new WaveDefinition("c 5 e 3 g 3 e 3 s 3 t", Rank.ELITE),
                    new WaveDefinition("s", Rank.BOSS),
                    new WaveDefinition("warden1", Rank.BOSS)),
            STARTING_CREDITS, STARTING_LIVES, SMOOTHING);

    private CurlyPathLevel() {
    }
}
