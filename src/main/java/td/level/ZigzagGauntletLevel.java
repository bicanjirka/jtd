package td.level;

import td.enemy.Rank;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

/**
 * A tighter, smoothly curving single lane on a smaller 12x10 board, 8 waves, starting with $75
 * and only 3 lives.
 */
final class ZigzagGauntletLevel {

    // cornerPull=0.3 keeps a comfortable margin under the tightest corner's leg (the shortest
    // is 2 cells = 64px, so pullback there is ~19px); 8 samples per corner is plenty smooth at
    // this board's scale without generating an excessive number of extra path points.
    static final LevelDefinition DEFINITION = LevelDefinition.singlePath(
            "Zigzag Gauntlet",
            "A tighter, smoothly curving path on a smaller board. 8 waves, starting with $75 and only 3 lives.",
            12, 10,
            List.of(
                    new Point(-1, 5), new Point(3, 5), new Point(3, 8), new Point(7, 8),
                    new Point(10, 2), new Point(10, 9), new Point(12, 9)),
            List.of(
                    new WaveDefinition("c e c e c e c", Rank.GRUNT),
                    new WaveDefinition("5 c", Rank.GRUNT),
                    new WaveDefinition("s e s e s", Rank.SOLDIER),
                    new WaveDefinition("t e t e t e t", Rank.SOLDIER),
                    new WaveDefinition("3 s 2 e 3 c", Rank.SOLDIER),
                    new WaveDefinition("g e g e g", Rank.VETERAN),
                    new WaveDefinition("2 g 2 t 2 s 2 c", Rank.VETERAN),
                    new WaveDefinition("10 c e 5 s e 3 t e g", Rank.ELITE)),
            75, 3,
            new ArcCornerSmoothing(0.3, 8));

    private ZigzagGauntletLevel() {
    }
}
