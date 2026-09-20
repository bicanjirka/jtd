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
import td.wave.smoothing.QuadraticBezierSmoothing;

import java.util.List;

/**
 * Three lanes twist through the same tall, narrow board: a center lane pinches into two stacked
 * round lobes, reading as an actual hourglass silhouette, while the two side lanes weave through
 * both pinch bands on their way from one edge to the other. A 9x14 portrait board, 10 waves per
 * lane, starting with $100 and 4 lives. Also this level's one level-authored enemy, the Reaver
 * (see {@link #REAVER}), still riding the crimson lane it always has.
 */
final class TwistedHourglassLevel {

    // This level's one level-authored enemy (LevelDefinition.customRankedEnemies, see
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

    // Every lane shares the same Bezier fillet: this board's legs are short (as little as 1
    // cell), so cornerPull is much gentler than a sprawling board could afford - 0.28 over the
    // shortest 1-cell leg (32px) pulls back 8.96px on each side, leaving a 14px gap between two
    // corners that share it rather than meeting.
    private static final QuadraticBezierSmoothing SMOOTHING = new QuadraticBezierSmoothing(0.28, 12);

    private static final PathDefinition TEAL_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 1), new Point(7, 1), new Point(7, 5),
                            new Point(5, 7), new Point(7, 9), new Point(7, 13),
                            new Point(-1, 13)),
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
                    SMOOTHING)
            .withColor(PathColor.of(30, 150, 140));

    private static final PathDefinition AMBER_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(2, -1), new Point(2, 5), new Point(5, 7),
                            new Point(2, 9), new Point(2, 14)),
                    List.of(
                            new WaveDefinition("c e c e c", Rank.GRUNT),
                            new WaveDefinition("6 c", Rank.GRUNT),
                            new WaveDefinition("s e s e s", Rank.GRUNT),
                            new WaveDefinition("t e t e t", Rank.SOLDIER),
                            new WaveDefinition("4 s 2 e 4 c", Rank.SOLDIER),
                            new WaveDefinition("g e g e g", Rank.SOLDIER),
                            new WaveDefinition("2 g 3 t", Rank.VETERAN),
                            new WaveDefinition("8 c e 6 s", Rank.VETERAN),
                            new WaveDefinition("3 g 2 t 2 g", Rank.ELITE),
                            new WaveDefinition("s", Rank.ELITE)),
                    SMOOTHING)
            .withColor(PathColor.of(230, 160, 40));

    private static final PathDefinition CRIMSON_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(9, 5), new Point(2, 5), new Point(0, 7),
                            new Point(2, 9), new Point(9, 9)),
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
                    SMOOTHING)
            .withColor(PathColor.of(200, 30, 60));

    static final LevelDefinition DEFINITION = LevelDefinition
            .of("Twisted Hourglass", 9, 14, List.of(CRIMSON_PATH, TEAL_PATH, AMBER_PATH))
            .withDescription("Three lanes twist through the same knot of cells, pinched together twice on their "
                    + "way from one edge of the board to another. 10 waves per lane, starting with $100 and 4 lives.")
            .withStartingCredits(100)
            .withStartingLives(4)
            .withCustomRankedEnemies(List.of(REAVER));

    private TwistedHourglassLevel() {
    }
}
