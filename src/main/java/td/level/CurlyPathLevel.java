package td.level;

import td.enemy.Rank;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;

/**
 * A single lane that spirals through two tight loops on the left half of the board before
 * unwinding into a steady zigzag on the way to the exit - a 20x15 board, 17 waves ending in the
 * Warden boss encounter. Gently rounded (the same {@link ArcCornerSmoothing} pull Zigzag Path
 * used to carry alone), not the sharp right angles the original version of this level had.
 */
final class CurlyPathLevel {

    // cornerPull=0.22 over this path's shortest leg (1 cell = 32px, so pullback there is ~7px)
    // is tighter than Zigzag Path's 0.4 - just enough to take the edge off every turn without
    // rounding the loops into circles, so they still read as square coils the way the design
    // sketch draws them.
    static final LevelDefinition DEFINITION = LevelDefinition.singlePath(
            "Curly Path",
            "A lane that spirals through two tight loops before unwinding into a steady zigzag. "
                    + "17 waves, starting with $50.",
            20, 15,
            List.of(
                    new Point(-1, 11), new Point(2, 11), new Point(2, 2), new Point(6, 2),
                    new Point(6, 8), new Point(3, 8), new Point(3, 5), new Point(7, 5),
                    new Point(7, 10), new Point(10, 10), new Point(10, 4), new Point(8, 4),
                    new Point(8, 7), new Point(13, 7), new Point(13, 2), new Point(16, 2),
                    new Point(16, 11), new Point(18, 11), new Point(18, 7), new Point(20, 7)),
            List.of(
                    new WaveDefinition("c e c e c e c e c", Rank.GRUNT),
                    new WaveDefinition("c e 2 c e 3 c e 4 c", Rank.GRUNT),
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
                    // The Warden boss - this wave slot is what constructs its first appearance,
                    // reading BuiltInEnemies.WARDEN_1's own baseHealth/price directly (every later
                    // stage, reached only via its egg hatching, is ability-spawned and reads its
                    // own stage's fields the same way). Rank.BOSS matches the wave immediately
                    // before it; the Warden's own BodyArchetype gives it a fixed, always-large
                    // body size (DefinedEnemyMob.bodyScaleFor) regardless of rank.
                    new WaveDefinition("warden1", Rank.BOSS)),
            50, 5,
            new ArcCornerSmoothing(0.22, 8));

    private CurlyPathLevel() {
    }
}
