package td.level;

import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.QuadraticBezierSmoothing;

import java.util.List;

/**
 * The Java-code source of levels - the only {@link LevelCatalog} implementation
 * that exists today. A future file-based catalog implements the same interface.
 */
public class BuiltInLevelCatalog implements LevelCatalog {

    private static final LevelDefinition CLASSIC_LOOP = LevelDefinition.unsmoothed(
            "Classic Loop",
            "The original winding path. 17 waves, starting with $50.",
            20, 15,
            List.of(
                    new Point(-1, 11), new Point(5, 11), new Point(5, 12), new Point(7, 12),
                    new Point(7, 6), new Point(4, 6), new Point(4, 5), new Point(3, 5),
                    new Point(3, 2), new Point(6, 2), new Point(6, 3), new Point(11, 3),
                    new Point(11, 5), new Point(14, 5), new Point(14, 3), new Point(17, 3),
                    new Point(17, 6), new Point(15, 6), new Point(15, 9), new Point(12, 9),
                    new Point(12, 12), new Point(20, 12)),
            List.of(
                    new WaveDefinition("c e c e c e c e c", 251, 2, 1),
                    new WaveDefinition("c e 2 c e 3 c e 4 c", 377, 3, 1),
                    new WaveDefinition("c e c", 812, 10, 2),
                    new WaveDefinition("4 c 2 e 2 s", 747, 5, 1),
                    new WaveDefinition("c c e s", 1109, 15, 3),
                    new WaveDefinition("10 c", 953, 2, 1),
                    new WaveDefinition("3 s e 4 c t e s t", 1117, 4, 2),
                    new WaveDefinition("2 c e e t", 2193, 15, 4),
                    new WaveDefinition("g 2 e 2 s", 1493, 10, 2),
                    new WaveDefinition("s t s c g c t c s g t c s g c t s g t c", 1476, 2, 2),
                    new WaveDefinition("g c g", 3789, 15, 4),
                    new WaveDefinition("6 g 2 e 4 t", 3088, 7, 3),
                    new WaveDefinition("c e c e c e c e c", 2912, 1, 2),
                    new WaveDefinition("2 s 3 t 2 g 4 e c", 3242, 10, 3),
                    new WaveDefinition("s 4 e t", 4014, 50, 6),
                    new WaveDefinition("c 5 e 3 g 3 e 3 s 3 t", 4016, 4, 4),
                    new WaveDefinition("s", 4751, 0, 8)),
            50, 5);

    // cornerPull=0.3 keeps a comfortable margin under the tightest corner's leg (the shortest
    // is 2 cells = 64px, so pullback there is ~19px); 8 samples per corner is plenty smooth at
    // this board's scale without generating an excessive number of extra path points.
    private static final LevelDefinition ZIGZAG_GAUNTLET = new LevelDefinition(
            "Zigzag Gauntlet",
            "A tighter, smoothly curving path on a smaller board. 8 waves, starting with $75 and only 3 lives.",
            12, 10,
            List.of(
                    new Point(-1, 5), new Point(3, 5), new Point(3, 8), new Point(7, 8),
                    new Point(7, 2), new Point(10, 2), new Point(10, 9), new Point(12, 9)),
            List.of(
                    new WaveDefinition("c e c e c e c", 200, 2, 1),
                    new WaveDefinition("5 c", 280, 2, 1),
                    new WaveDefinition("s e s e s", 450, 4, 2),
                    new WaveDefinition("t e t e t e t", 600, 3, 2),
                    new WaveDefinition("3 s 2 e 3 c", 900, 3, 2),
                    new WaveDefinition("g e g e g", 1200, 8, 3),
                    new WaveDefinition("2 g 2 t 2 s 2 c", 1800, 5, 3),
                    new WaveDefinition("10 c e 5 s e 3 t e g", 2600, 4, 4)),
            75, 3,
            new QuadraticBezierSmoothing(0.3, 8));

    @Override
    public List<LevelDefinition> levels() {
        return List.of(CLASSIC_LOOP, ZIGZAG_GAUNTLET);
    }
}
