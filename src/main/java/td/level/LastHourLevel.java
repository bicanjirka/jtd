package td.level;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.CrumbleTrait;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.OnDeathTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.PeriodicTrigger;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.RankGrowth;
import td.enemy.RankedEnemy;
import td.enemy.RewindAction;
import td.enemy.RotorMovement;
import td.enemy.SelfTarget;
import td.enemy.SpawnEnemiesAction;
import td.enemy.SwingCurve;
import td.enemy.SwingMovement;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.QuadraticBezierSmoothing;

import java.util.List;

/**
 * The Last Hour: three lanes through the waist of an hourglass, and an enemy that is time running out. Sandlings
 * crumble on their own, so what holds them back is worth more than what hits them; Timekeepers turn their allies'
 * health back; Glass Sentinels shrug off small hits and shatter under crits; and at the end the Keeper of Hours,
 * who turns back what was done to it unless it is stopped from casting.
 */
final class LastHourLevel {

    private static final String LEVEL_NAME = "The Last Hour";
    private static final String LEVEL_DESCRIPTION = "Sand falls through the waist of the glass, and the hour runs out "
            + "with it. Hold the sand still - and never let the Keeper turn it back.";
    private static final int STARTING_CREDITS = 100;
    private static final int STARTING_LIVES = 4;

    /**
     * A grain of the hour: quick, and crumbling away on its own about as fast as it crosses the board, at every rank,
     * so a slowed Sandling never arrives.
     */
    private static final RankedEnemy SANDLING = RankedEnemy
            .startingAt(EnemyDefinition.of("sandling", "Sandling", 70, 1, 1.5f, BodyArchetype.SANDLING)
                    .withDescription("A handful of the hour's sand, running down the glass. Hold it back and it is gone.")
                    .withMovement(new PathDirectionalMovement())
                    .withAgility(10f)
                    .withIdentifiedTraits(List.of(crumble(4f))), RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalTraits(List.of(crumble(7f))))
            .thenAt(Rank.VETERAN, e -> e.withAdditionalTraits(List.of(crumble(14f))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(5f, 5f, 0f, 0f, 0f, 25f, 0f)
                    .withAdditionalTraits(List.of(crumble(30f))))
            .thenAt(Rank.BOSS, e -> e.withAdditionalTraits(List.of(crumble(50f))))
            .build();

    /** A walking clock that turns the health of the allies around it back, further with rank. */
    private static final RankedEnemy TIMEKEEPER = RankedEnemy
            .startingAt(EnemyDefinition.of("timekeeper", "Timekeeper", 130, 3, 1.0f, BodyArchetype.TIMEKEEPER)
                    .withDescription("Keeps the hour for the others: whatever they lose, it gives back.")
                    .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                    .withIdentifiedAbilities(List.of(rewindAround(60))), RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalAbilities(List.of(rewindAround(70))))
            .thenAt(Rank.VETERAN, e -> e.withAdditionalAbilities(List.of(rewindAround(80))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(10f, 20f, 0f, 0f, 0f, 10f, 20f)
                    .withAdditionalAbilities(List.of(rewindAround(100))))
            .thenAt(Rank.BOSS, e -> e.withAdditionalAbilities(List.of(rewindAround(120))))
            .build();

    /** Glass: thick against every small hit, either type, and shattering under a crit. */
    private static final RankedEnemy GLASS_SENTINEL = RankedEnemy
            .startingAt(EnemyDefinition.of("glassSentinel", "Glass Sentinel", 260, 4, 0.8f, BodyArchetype.GLASS_SENTINEL)
                    .withDescription("Cut from the glass itself. A small hit only rings on it; a hard one shatters it.")
                    .withMovement(new SwingMovement((float) Math.toRadians(6.0), 40, SwingCurve.SMOOTH))
                    .withStats(0f, 0f, 8f, 8f, -60f, 0f, 0f), RankGrowth.of(16f))
            .thenAt(Rank.SOLDIER, e -> e.withPlating(10f).withInsulation(10f))
            .thenAt(Rank.VETERAN, e -> e.withPlating(13f).withInsulation(13f))
            .thenAt(Rank.ELITE, e -> e.withStats(10f, 10f, 16f, 16f, -60f, 0f, 10f))
            .thenAt(Rank.BOSS, e -> e.withStats(20f, 20f, 22f, 22f, -50f, 0f, 20f))
            .build();

    /** A tenth of the rank's health, once it is badly wounded. */
    private static IdentifiedAbility panicShield(float points) {
        return IdentifiedAbility.named("panicShield", new Ability(AbilityTerm.PANIC, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(new ShieldTemplate(points, 100), new SelfTarget())));
    }

    /** A brute of packed sand that runs faster as it is hurt and falls apart into Sandlings. */
    private static final RankedEnemy REAVER = RankedEnemy
            .startingAt(EnemyDefinition.of("sandReaver", "Sand Reaver", 750, 6, 1.28f, BodyArchetype.REAVER)
                    .withDescription("Sand packed hard into a brute. It runs faster as it is hurt, and when it falls, "
                            + "it falls apart.")
                    .withMovement(new PathDirectionalMovement())
                    .withHealthDivisor(0.8f)
                    .withPlating(1f)
                    .withInsulation(1f)
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.3f))))
                    .withIdentifiedAbilities(List.of(
                            panicShield(100f),
                            IdentifiedAbility.named("splitOnDeath",
                                    new Ability(AbilityTerm.SPLIT, new OnDeathTrigger(), new SpawnEnemiesAction("sandling", 2, false))))),
                    RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e
                    .withAdditionalAbilities(List.of(panicShield(180f))))
            .thenAt(Rank.VETERAN, e -> e
                    .withAdditionalAbilities(List.of(panicShield(320f))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(10f, 10f, 1f, 1f, 100f, 5f, 10f)
                    .withAdditionalAbilities(List.of(panicShield(580f))))
            .thenAt(Rank.BOSS, e -> e
                    .withStats(15f, 15f, 1f, 1f, 100f, 5f, 20f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.6f))))
                    .withAdditionalAbilities(List.of(panicShield(1050f))))
            .build();

    private static final QuadraticBezierSmoothing SMOOTHING = new QuadraticBezierSmoothing(0.28, 12);

    /** Into the glass from the top left, down through the waist and out at the bottom. */
    private static final PathColor GLASS_COLOR = PathColor.of(140, 215, 225);
    private static final PathDefinition GLASS_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 1), new Point(7, 1), new Point(7, 5),
                            new Point(5, 7), new Point(2, 9), new Point(2, 15)),
                    List.of(
                            new WaveDefinition("swarm 8 sandling e 4 walker", Rank.GRUNT),
                            new WaveDefinition("column 4 knight e swarm 8 sandling", Rank.GRUNT),
                            new WaveDefinition("timekeeper column 5 walker e swarm 8 sandling", Rank.GRUNT),
                            new WaveDefinition("2 glassSentinel e column 6 walker", Rank.SOLDIER),
                            new WaveDefinition("swarm 12 sandling e timekeeper 4 runner", Rank.SOLDIER),
                            new WaveDefinition("timekeeper 2 glassSentinel e column 6 knight", Rank.SOLDIER),
                            new WaveDefinition("swarm 12 sandling e swarm 12 sandling", Rank.VETERAN),
                            new WaveDefinition("3 glassSentinel timekeeper e 4 runner", Rank.VETERAN),
                            new WaveDefinition("elite timekeeper column 8 walker e swarm 12 sandling", Rank.VETERAN),
                            new WaveDefinition("drip 6 courier e 2 timekeeper 2 glassSentinel", Rank.VETERAN),
                            new WaveDefinition("swarm 12 sandling e timekeeper column 6 knight", Rank.ELITE),
                            new WaveDefinition("6 e elite timekeeper swarm 12 sandling e 2 elite glassSentinel", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(GLASS_COLOR);

    /** Into the glass from the top, crossing the glass lane at the waist and out to the left. */
    private static final PathColor SAND_COLOR = PathColor.of(235, 195, 110);
    private static final PathDefinition SAND_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(2, -1), new Point(2, 5), new Point(5, 7),
                            new Point(7, 9), new Point(7, 13), new Point(-1, 13)),
                    List.of(
                            new WaveDefinition("6 walker e swarm 6 sandling", Rank.GRUNT),
                            new WaveDefinition("glassSentinel e 4 walker", Rank.GRUNT),
                            new WaveDefinition("swarm 10 sandling e 2 runner", Rank.GRUNT),
                            new WaveDefinition("timekeeper column 6 knight", Rank.SOLDIER),
                            new WaveDefinition("3 glassSentinel e swarm 8 sandling", Rank.SOLDIER),
                            new WaveDefinition("drip 4 courier e swarm 12 sandling", Rank.SOLDIER),
                            new WaveDefinition("2 timekeeper column 6 walker e glassSentinel", Rank.VETERAN),
                            new WaveDefinition("golem e timekeeper column 6 knight", Rank.VETERAN),
                            new WaveDefinition("swarm 12 sandling e 3 glassSentinel", Rank.VETERAN),
                            new WaveDefinition("elite glassSentinel 2 glassSentinel e swarm 12 sandling", Rank.VETERAN),
                            new WaveDefinition("3 glassSentinel e timekeeper swarm 12 sandling", Rank.ELITE),
                            new WaveDefinition("8 e swarm 12 sandling e elite timekeeper 4 knight", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(SAND_COLOR);

    /** Out of the waist itself, a short loop round the left of the glass and back across it. */
    private static final PathColor DUSK_COLOR = PathColor.of(230, 110, 90);
    private static final PathDefinition DUSK_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(9, 5), new Point(2, 5), new Point(0, 7),
                            new Point(2, 9), new Point(9, 9)),
                    List.of(
                            new WaveDefinition("3 sandling", Rank.GRUNT),
                            new WaveDefinition("swarm 6 sandling", Rank.GRUNT),
                            new WaveDefinition("sandReaver", Rank.GRUNT),
                            new WaveDefinition("swarm 10 sandling e sandReaver", Rank.SOLDIER),
                            new WaveDefinition("2 sandReaver", Rank.SOLDIER),
                            new WaveDefinition("timekeeper swarm 10 sandling", Rank.SOLDIER),
                            new WaveDefinition("sandReaver e swarm 10 sandling", Rank.VETERAN),
                            new WaveDefinition("2 sandReaver timekeeper", Rank.VETERAN),
                            new WaveDefinition("elite sandReaver e swarm 12 sandling", Rank.VETERAN),
                            new WaveDefinition("2 timekeeper column 6 knight", Rank.VETERAN),
                            new WaveDefinition("2 sandReaver e swarm 12 sandling", Rank.ELITE),
                            new WaveDefinition("keeperOfHours e 2 timekeeper", Rank.ELITE)),
                    SMOOTHING)
            .withColor(DUSK_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 9, 15, List.of(DUSK_PATH, GLASS_PATH, SAND_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withCardColor(GLASS_COLOR)
            .withGround(BoardGround.of(0x070C0E, GroundMotif.DUNES, 0x111C1E))
            // What the Reaver and the Keeper call, before them.
            .withCustomRankedEnemies(List.of(SANDLING, TIMEKEEPER, GLASS_SENTINEL, REAVER, KeeperEnemy.keeper()));

    private LastHourLevel() {
    }

    /** Loses {@code pointsPerSecond} health a second, replacing what an earlier rank lost. */
    private static IdentifiedTrait crumble(float pointsPerSecond) {
        return IdentifiedTrait.named("crumble", new CrumbleTrait(pointsPerSecond));
    }

    /** Every seven seconds, turns the health of the allies near it back {@code windowTicks}. */
    private static IdentifiedAbility rewindAround(int windowTicks) {
        return IdentifiedAbility.named("rewind", new Ability(AbilityTerm.REWIND, new PeriodicTrigger(140),
                new RewindAction(windowTicks, new RadiusTarget(90f))));
    }
}
