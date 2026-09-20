package td.level;

import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.FlatResistTrait;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.OnDeathTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.Rank;
import td.enemy.RankedEnemy;
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
 * Long bezier curves that swing far wide of the path's straight corners, crossed by a second,
 * faster lane sweeping top to bottom - a 30x18 board, 10 waves per lane, starting with $100 and
 * 4 lives. Also this level's one level-authored enemy, the Reaver (see {@link #REAVER}).
 */
final class WildBezierSweepLevel {

    // Wild Bezier Sweep's one level-authored enemy (LevelDefinition.customRankedEnemies, see
    // td/level/CLAUDE.md) - deliberately reaches for two options no built-in enemy exercises
    // yet: PathDirectionalMovement, so its triangle nose actually tracks the sweeping curve
    // instead of spinning in place the way Frenzied's RotorMovement does, and a healthDivisor
    // below 1, the opposite direction from Ghost's flat /5 - it makes a Reaver tougher than its
    // own base health rather than weaker. HurtSpeedTrait/FlatResistTrait combine a Frenzied-
    // style speed curve with a Warden-style flat reduction, at values scaled down to a wave-1
    // appearance rather than a boss encounter; the health-threshold panic shield and the
    // on-death split are both real Ability/AbilityAction wiring, just aimed at itself and at a
    // "spawn two, don't replace" reinforcement instead of the Warden's "shield allies"/"replace
    // with the next stage" uses of the same two mechanisms. A full five-rank ladder, like the
    // five basic built-ins - traits/abilities/movement stay the same across ranks, only health
    // and bounty escalate, the same pattern ARMORED/FRENZIED/GHOST use.
    private static final RankedEnemy REAVER = RankedEnemy
            .startingAt(EnemyDefinition.of("reaver", "Reaver", 500, 12, 1.28f, BodyArchetype.TRIANGLE)
                    .withDescription("Speeds up as it's hurt, shrugs off a flat amount of every hit, panics into a "
                            + "brief shield once badly wounded, and splits into two stragglers when finally brought down.")
                    .withMovement(new PathDirectionalMovement())
                    .withHealthDivisor(0.8f)
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.3f)),
                            IdentifiedTrait.named("armor", new FlatResistTrait(10))))
                    .withIdentifiedAbilities(List.of(
                            IdentifiedAbility.named("panicShield", new Ability(new HealthThresholdTrigger(0.5f),
                                    new ApplyEffectAction(new ShieldTemplate(0.25f, 100), new SelfTarget()))),
                            IdentifiedAbility.named("splitOnDeath",
                                    new Ability(new OnDeathTrigger(), new SpawnEnemiesAction("c", 2, false))))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(1000, 20))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(2000, 32))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(4000, 50))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(8000, 80))
            .build();

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
    private static final PathDefinition PATH_A = PathDefinition.smoothed(
            List.of(
                    new Point(-1, 3), new Point(14, 3), new Point(14, 15), new Point(28, 15),
                    new Point(28, 4), new Point(30, 4)),
            List.of(
                    new WaveDefinition("c e c e reaver e c e c", Rank.GRUNT),
                    new WaveDefinition("6 c", Rank.GRUNT),
                    new WaveDefinition("s e s e s", Rank.GRUNT),
                    new WaveDefinition("t e t e t", Rank.SOLDIER),
                    new WaveDefinition("4 s 2 e 4 c", Rank.SOLDIER),
                    new WaveDefinition("g e g e g", Rank.SOLDIER),
                    new WaveDefinition("2 g 3 t 3 s", Rank.VETERAN),
                    new WaveDefinition("8 c e 6 s e 4 t", Rank.VETERAN),
                    new WaveDefinition("3 g 2 t 2 g 2 t", Rank.ELITE),
                    new WaveDefinition("s", Rank.BOSS)),
            new QuadraticBezierSmoothing(0.5, 14))
            .withColor(PathColor.of(230, 170, 60));

    // A circular fillet rather than a Bezier - a genuinely different curve character from Path
    // A's, at a cornerPull comfortably under half of every adjacent leg's length (the shortest,
    // the 6-cell legs either side of each interior corner, allows up to 3 cells of pullback).
    // withSpeed(1.3f) is this level's one called-out "faster by default" lane; every other
    // built-in path stays at the 1x default.
    private static final PathDefinition PATH_B = PathDefinition.smoothed(
            List.of(
                    new Point(5, -1), new Point(5, 5), new Point(22, 5), new Point(22, 11),
                    new Point(9, 11), new Point(9, 18)),
            List.of(
                    new WaveDefinition("c e c e c e c", Rank.GRUNT),
                    new WaveDefinition("s e s e s", Rank.GRUNT),
                    new WaveDefinition("5 t", Rank.SOLDIER),
                    new WaveDefinition("g e g e g e g", Rank.SOLDIER),
                    new WaveDefinition("line 3 c e line 3 s", Rank.SOLDIER),
                    new WaveDefinition("flank c e flank t", Rank.VETERAN),
                    new WaveDefinition("swarm 4 c e swarm 4 s", Rank.VETERAN),
                    new WaveDefinition("column 3 t e column 3 g", Rank.VETERAN),
                    new WaveDefinition("drip 4 s e drip 4 t", Rank.ELITE),
                    new WaveDefinition("3 g 3 t 3 s 3 c", Rank.ELITE)),
            new ArcCornerSmoothing(0.4, 10))
            .withColor(PathColor.of(90, 190, 230))
            .withSpeed(1.3f);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of("Wild Bezier Sweep", 30, 18, List.of(PATH_A, PATH_B))
            .withDescription("Long bezier curves that swing far wide of the path's straight corners, crossed by a "
                    + "second, faster lane sweeping top to bottom. 10 waves per lane, starting with "
                    + "$100 and 4 lives.")
            .withStartingLives(4)
            .withCustomRankedEnemies(List.of(REAVER));

    private WildBezierSweepLevel() {
    }
}
