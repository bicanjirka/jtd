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

final class TwistedHourglassLevel {

    private static final String LEVEL_NAME = "Twisted Hourglass";
    private static final String LEVEL_DESCRIPTION = "Three lanes twist through the same knot of cells, pinched together twice on their "
            + "way from one edge of the board to another.";
    private static final int STARTING_CREDITS = 100;
    private static final int STARTING_LIVES = 4;

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

    private static final QuadraticBezierSmoothing SMOOTHING = new QuadraticBezierSmoothing(0.28, 12);

    private static final PathColor TEAL_COLOR = PathColor.of(30, 150, 140);
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
            .withColor(TEAL_COLOR);

    private static final PathColor AMBER_COLOR = PathColor.of(230, 160, 40);
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
            .withColor(AMBER_COLOR);

    private static final PathColor CRIMSON_COLOR = PathColor.of(200, 30, 60);
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
            .withColor(CRIMSON_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 9, 14, List.of(CRIMSON_PATH, TEAL_PATH, AMBER_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withCustomRankedEnemies(List.of(REAVER));

    private TwistedHourglassLevel() {
    }
}
