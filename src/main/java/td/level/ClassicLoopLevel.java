package td.level;

import td.enemy.Rank;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.List;

/**
 * The original winding path - a single, unsmoothed lane on a 20x15 board, 17 waves ending in
 * the Warden boss encounter.
 */
final class ClassicLoopLevel {

    static final LevelDefinition DEFINITION = LevelDefinition.unsmoothed(
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
            50, 5);

    private ClassicLoopLevel() {
    }
}
