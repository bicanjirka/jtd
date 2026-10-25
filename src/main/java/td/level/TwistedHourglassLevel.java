package td.level;

import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
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

    /** A tenth of the rank's health, once it is badly wounded. */
    private static IdentifiedAbility panicShield(float points) {
        return IdentifiedAbility.named("panicShield", new Ability(AbilityTerm.PANIC, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(new ShieldTemplate(points, 100), new SelfTarget())));
    }

    private static final RankedEnemy REAVER = RankedEnemy
            .startingAt(EnemyDefinition.of("reaver", "Reaver", 750, 6, 1.28f, BodyArchetype.TRIANGLE)
                    .withDescription("A brute that runs faster as it is hurt and splits when it falls.")
                    .withMovement(new PathDirectionalMovement())
                    .withHealthDivisor(0.8f)
                    .withPlating(1f)
                    .withInsulation(1f)
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.3f))))
                    .withIdentifiedAbilities(List.of(
                            panicShield(100f),
                            IdentifiedAbility.named("splitOnDeath",
                                    new Ability(AbilityTerm.SPLIT, new OnDeathTrigger(), new SpawnEnemiesAction("simple", 2, false))))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(1350, 10)
                    .withAdditionalAbilities(List.of(panicShield(180f))))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(2430, 14)
                    .withAdditionalAbilities(List.of(panicShield(320f))))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(4374, 26)
                    .withStats(10f, 10f, 1f, 1f, 100f, 5f, 10f)
                    .withAdditionalAbilities(List.of(panicShield(580f))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(7874, 46)
                    .withStats(15f, 15f, 1f, 1f, 100f, 5f, 20f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.6f))))
                    .withAdditionalAbilities(List.of(panicShield(1050f))))
            .build();

    private static final QuadraticBezierSmoothing SMOOTHING = new QuadraticBezierSmoothing(0.28, 12);

    private static final PathColor TEAL_COLOR = PathColor.of(30, 150, 140);
    private static final PathDefinition TEAL_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 1), new Point(7, 1), new Point(7, 5),
                            new Point(5, 7), new Point(2, 9), new Point(2, 15)),
                    List.of(
                            new WaveDefinition("simple e simple e simple e simple e plated simple e 4 simple e 3 armored", Rank.GRUNT),
                            new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.GRUNT),
                            new WaveDefinition("5 fast e 4 simple e 2 armored e 2 fast", Rank.SOLDIER),
                            new WaveDefinition("ghost e ghost e ghost e ghost e 3 armored e 3 simple", Rank.SOLDIER),
                            new WaveDefinition("line 3 simple e line 3 armored e line 4 simple e line 2 fast", Rank.SOLDIER),
                            new WaveDefinition("flank simple e flank fast e 4 simple e flank armored", Rank.VETERAN),
                            new WaveDefinition("swarm 4 simple e swarm 4 armored e swarm 4 fast e 3 ghost", Rank.VETERAN),
                            new WaveDefinition("column 3 fast e column 3 ghost e column 4 simple e column 2 armored", Rank.VETERAN),
                            new WaveDefinition("drip 4 armored e drip 4 fast e drip 4 simple e 2 ghost", Rank.ELITE),
                            new WaveDefinition("3 ghost 3 fast 3 armored 3 simple 2 e 4 ghost 4 fast 4 armored", Rank.ELITE)),
                    SMOOTHING)
            .withColor(TEAL_COLOR);

    private static final PathColor AMBER_COLOR = PathColor.of(230, 160, 40);
    private static final PathDefinition AMBER_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(2, -1), new Point(2, 5), new Point(5, 7),
                            new Point(7, 9), new Point(7, 13), new Point(-1, 13)),
                    List.of(
                            new WaveDefinition("simple e simple e simple e 4 simple e 3 armored", Rank.GRUNT),
                            new WaveDefinition("6 simple e 4 simple e 2 armored", Rank.GRUNT),
                            new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.GRUNT),
                            new WaveDefinition("fast e fast e fast e 4 simple e 2 armored e fast", Rank.SOLDIER),
                            new WaveDefinition("4 armored 2 e 4 simple 2 e 4 armored 2 e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("ghost e ghost e ghost e 3 armored e 3 simple e ghost", Rank.SOLDIER),
                            new WaveDefinition("2 ghost 3 fast 2 e 2 ghost 3 armored 3 simple", Rank.VETERAN),
                            new WaveDefinition("8 simple e 6 armored e 6 simple e 4 fast", Rank.VETERAN),
                            new WaveDefinition("3 ghost 2 fast 2 ghost 2 e 3 armored 3 fast 4 simple", Rank.ELITE),
                            new WaveDefinition("armored", Rank.ELITE)),
                    SMOOTHING)
            .withColor(AMBER_COLOR);

    private static final PathColor CRIMSON_COLOR = PathColor.of(200, 30, 60);
    private static final PathDefinition CRIMSON_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(9, 5), new Point(2, 5), new Point(0, 7),
                            new Point(2, 9), new Point(9, 9)),
                    List.of(
                            new WaveDefinition("simple e simple e reaver e simple e simple e 4 simple e 3 armored", Rank.GRUNT),
                            new WaveDefinition("6 simple e 4 simple e 2 armored", Rank.GRUNT),
                            new WaveDefinition("armored e armored e armored e 4 simple e armored e armored", Rank.GRUNT),
                            new WaveDefinition("fast e fast e fast e 4 simple e 2 armored e fast", Rank.SOLDIER),
                            new WaveDefinition("4 armored 2 e 4 simple 2 e 4 armored 2 e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("ghost e ghost e ghost e 3 armored e 3 simple e ghost", Rank.SOLDIER),
                            new WaveDefinition("2 ghost 3 fast 3 armored 2 e 2 ghost 3 armored 3 simple", Rank.VETERAN),
                            new WaveDefinition("8 simple e 6 armored e 4 fast e 6 simple e 4 armored", Rank.VETERAN),
                            new WaveDefinition("3 ghost 2 fast 2 ghost 2 fast 2 e 3 armored 3 fast 4 simple", Rank.ELITE),
                            new WaveDefinition("armored", Rank.BOSS)),
                    SMOOTHING)
            .withColor(CRIMSON_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 9, 15, List.of(CRIMSON_PATH, TEAL_PATH, AMBER_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withCustomRankedEnemies(List.of(REAVER));

    private TwistedHourglassLevel() {
    }
}
