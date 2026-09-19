package td.level;

import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.FlatResistTrait;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.OnDeathTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.Rank;
import td.enemy.SelfTarget;
import td.enemy.SpawnEnemiesAction;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;
import td.wave.smoothing.QuadraticBezierSmoothing;

import java.util.List;

/**
 * The Java-code source of levels - the only {@link LevelCatalog} implementation
 * that exists today. A future file-based catalog implements the same interface.
 */
public class BuiltInLevelCatalog implements LevelCatalog {

    // Wild Bezier Sweep's one level-authored enemy (LevelDefinition.customEnemies, see
    // td/level/CLAUDE.md) - deliberately reaches for two options no built-in enemy exercises
    // yet: PathDirectionalMovement, so its triangle nose actually tracks the sweeping curve
    // instead of spinning in place the way Frenzied's RotorMovement does, and a healthDivisor
    // below 1, the opposite direction from Ghost's flat /5 - it makes a Reaver tougher than its
    // wave's base health rather than weaker. HurtSpeedTrait/FlatResistTrait combine a Frenzied-
    // style speed curve with a Warden-style flat reduction, at values scaled down to a wave-1
    // appearance rather than a boss encounter; the health-threshold panic shield and the
    // on-death split are both real Ability/AbilityAction wiring, just aimed at itself and at a
    // "spawn two, don't replace" reinforcement instead of the Warden's "shield allies"/"replace
    // with the next stage" uses of the same two mechanisms.
    private static final EnemyDefinition REAVER = EnemyDefinition
            .of("reaver", "Reaver", 500, 12, 1.28f, BodyArchetype.TRIANGLE)
            .withDescription("Speeds up as it's hurt, shrugs off a flat amount of every hit, panics into a "
                    + "brief shield once badly wounded, and splits into two stragglers when finally brought down.")
            .withMovement(new PathDirectionalMovement())
            .withHealthDivisor(0.8f)
            .withTraits(List.of(new HurtSpeedTrait(1.3f), new FlatResistTrait(10)))
            .withAbilities(List.of(
                    new Ability(new HealthThresholdTrigger(0.5f),
                            new ApplyEffectAction(new ShieldTemplate(0.25f, 100), new SelfTarget())),
                    new Ability(new OnDeathTrigger(), new SpawnEnemiesAction("c", 2, false))));

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
                    new WaveDefinition("c e c e c e c e c", 251, 2, Rank.GRUNT),
                    new WaveDefinition("c e 2 c e 3 c e 4 c", 377, 3, Rank.GRUNT),
                    new WaveDefinition("c e c", 812, 10, Rank.SOLDIER),
                    new WaveDefinition("4 c 2 e 2 s", 747, 5, Rank.GRUNT),
                    new WaveDefinition("c c e s", 1109, 15, Rank.VETERAN),
                    new WaveDefinition("10 c", 953, 2, Rank.GRUNT),
                    new WaveDefinition("3 s e 4 c t e s t", 1117, 4, Rank.SOLDIER),
                    new WaveDefinition("2 c e e t", 2193, 15, Rank.ELITE),
                    new WaveDefinition("g 2 e 2 s", 1493, 10, Rank.SOLDIER),
                    new WaveDefinition("s t s c g c t c s g t c s g c t s g t c", 1476, 2, Rank.SOLDIER),
                    new WaveDefinition("g c g", 3789, 15, Rank.ELITE),
                    new WaveDefinition("6 g 2 e 4 t", 3088, 7, Rank.VETERAN),
                    new WaveDefinition("c e c e c e c e c", 2912, 1, Rank.SOLDIER),
                    new WaveDefinition("2 s 3 t 2 g 4 e c", 3242, 10, Rank.VETERAN),
                    new WaveDefinition("s 4 e t", 4014, 50, Rank.BOSS),
                    new WaveDefinition("c 5 e 3 g 3 e 3 s 3 t", 4016, 4, Rank.ELITE),
                    new WaveDefinition("s", 4751, 0, Rank.BOSS),
                    // The Warden boss - hp/price here deliberately match BuiltInEnemies.WARDEN_1's
                    // own baseHealth/price, since this wave slot is what constructs its first
                    // appearance (every later stage, reached only via its egg hatching, is
                    // ability-spawned and reads those same fields directly instead - see
                    // EnemyDefinition's own doc comment). Rank.BOSS, matching the wave immediately
                    // before it: the Warden's own BodyArchetype gives it a fixed, always-large
                    // body size (DefinedEnemyMob.bodyScaleFor), so this rank doesn't need to
                    // inflate its body the way it would if that formula still scaled with it.
                    new WaveDefinition("warden1", 8000, 100, Rank.BOSS)),
            50, 5);

    // cornerPull=0.3 keeps a comfortable margin under the tightest corner's leg (the shortest
    // is 2 cells = 64px, so pullback there is ~19px); 8 samples per corner is plenty smooth at
    // this board's scale without generating an excessive number of extra path points.
    private static final LevelDefinition ZIGZAG_GAUNTLET = LevelDefinition.singlePath(
            "Zigzag Gauntlet",
            "A tighter, smoothly curving path on a smaller board. 8 waves, starting with $75 and only 3 lives.",
            12, 10,
            List.of(
                    new Point(-1, 5), new Point(3, 5), new Point(3, 8), new Point(7, 8),
                    new Point(10, 2), new Point(10, 9), new Point(12, 9)),
            List.of(
                    new WaveDefinition("c e c e c e c", 200, 2, Rank.GRUNT),
                    new WaveDefinition("5 c", 280, 2, Rank.GRUNT),
                    new WaveDefinition("s e s e s", 450, 4, Rank.SOLDIER),
                    new WaveDefinition("t e t e t e t", 600, 3, Rank.SOLDIER),
                    new WaveDefinition("3 s 2 e 3 c", 900, 3, Rank.SOLDIER),
                    new WaveDefinition("g e g e g", 1200, 8, Rank.VETERAN),
                    new WaveDefinition("2 g 2 t 2 s 2 c", 1800, 5, Rank.VETERAN),
                    new WaveDefinition("10 c e 5 s e 3 t e g", 2600, 4, Rank.ELITE)),
            75, 3,
            new ArcCornerSmoothing(0.3, 8));

    // cornerPull is maxed out at 0.5 (the largest AbstractCornerSmoothing allows) and the three
    // interior corners each sit between two long (11-15 cell) legs, so each one pulls back
    // 5.5-6 cells before the Bezier fillet even starts - far more than Zigzag Gauntlet's 0.3
    // pull over 2-4 cell legs. The resulting curve cuts deep inside each raw right-angle corner,
    // sweeping the actual path through cells the straight two-leg corners never touch at all
    // (BuiltInLevelCatalogTest measures this directly against Zigzag Gauntlet's own path).
    //
    // This level's second lane (below) enters from the top edge and exits through the bottom,
    // deliberately crossing this lane's own corridor rather than running parallel to it - the
    // two lanes are independent paths, so a crossing is just a cell both happen to make
    // unbuildable, not a hazard to route around. See docs/features/FEATURE-multiple-enemy-paths.md.
    private static final PathDefinition WILD_BEZIER_SWEEP_PATH_A = PathDefinition.smoothed(
            List.of(
                    new Point(-1, 3), new Point(14, 3), new Point(14, 15), new Point(28, 15),
                    new Point(28, 4), new Point(30, 4)),
            List.of(
                    new WaveDefinition("c e c e reaver e c e c", 220, 2, Rank.GRUNT),
                    new WaveDefinition("6 c", 300, 2, Rank.GRUNT),
                    new WaveDefinition("s e s e s", 480, 3, Rank.GRUNT),
                    new WaveDefinition("t e t e t", 620, 3, Rank.SOLDIER),
                    new WaveDefinition("4 s 2 e 4 c", 850, 4, Rank.SOLDIER),
                    new WaveDefinition("g e g e g", 1100, 6, Rank.SOLDIER),
                    new WaveDefinition("2 g 3 t 3 s", 1450, 5, Rank.VETERAN),
                    new WaveDefinition("8 c e 6 s e 4 t", 1800, 4, Rank.VETERAN),
                    new WaveDefinition("3 g 2 t 2 g 2 t", 2400, 8, Rank.ELITE),
                    new WaveDefinition("s", 4200, 0, Rank.BOSS)),
            new QuadraticBezierSmoothing(0.5, 14))
            .withColor(PathColor.of(230, 170, 60));

    // A circular fillet rather than a Bezier - a genuinely different curve character from Path
    // A's, at a cornerPull comfortably under half of every adjacent leg's length (the shortest,
    // the 6-cell legs either side of each interior corner, allows up to 3 cells of pullback).
    // withSpeed(1.3f) is this level's one called-out "faster by default" lane; every other
    // built-in path stays at the 1x default.
    private static final PathDefinition WILD_BEZIER_SWEEP_PATH_B = PathDefinition.smoothed(
            List.of(
                    new Point(5, -1), new Point(5, 5), new Point(22, 5), new Point(22, 11),
                    new Point(9, 11), new Point(9, 18)),
            List.of(
                    new WaveDefinition("c e c e c e c", 180, 2, Rank.GRUNT),
                    new WaveDefinition("s e s e s", 260, 3, Rank.GRUNT),
                    new WaveDefinition("5 t", 340, 3, Rank.SOLDIER),
                    new WaveDefinition("g e g e g e g", 420, 4, Rank.SOLDIER),
                    new WaveDefinition("line 3 c e line 3 s", 520, 4, Rank.SOLDIER),
                    new WaveDefinition("flank c e flank t", 650, 5, Rank.VETERAN),
                    new WaveDefinition("swarm 4 c e swarm 4 s", 820, 5, Rank.VETERAN),
                    new WaveDefinition("column 3 t e column 3 g", 1050, 6, Rank.VETERAN),
                    new WaveDefinition("drip 4 s e drip 4 t", 1350, 7, Rank.ELITE),
                    new WaveDefinition("3 g 3 t 3 s 3 c", 1800, 8, Rank.ELITE)),
            new ArcCornerSmoothing(0.4, 10))
            .withColor(PathColor.of(90, 190, 230))
            .withSpeed(1.3f);

    private static final LevelDefinition WILD_BEZIER_SWEEP = LevelDefinition
            .of("Wild Bezier Sweep", 30, 18, List.of(WILD_BEZIER_SWEEP_PATH_A, WILD_BEZIER_SWEEP_PATH_B))
            .withDescription("Long bezier curves that swing far wide of the path's straight corners, crossed by a "
                    + "second, faster lane sweeping top to bottom. 10 waves per lane, starting with "
                    + "$100 and 4 lives.")
            .withStartingLives(4)
            .withCustomEnemies(List.of(REAVER));

    @Override
    public List<LevelDefinition> levels() {
        return List.of(CLASSIC_LOOP, ZIGZAG_GAUNTLET, WILD_BEZIER_SWEEP);
    }
}
