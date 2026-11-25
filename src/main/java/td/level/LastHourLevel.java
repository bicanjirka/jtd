package td.level;

import td.board.BoardGround;
import td.board.BoardLight;
import td.board.GroundMotif;
import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.enemy.Ability;
import td.enemy.AbilitySpawnShape;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.CrumbleTrait;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.ImmunityTrait;
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
    private static final int STARTING_CREDITS = 120;
    private static final int STARTING_LIVES = 4;

    /**
     * A grain of the hour: quick, and crumbling away on its own about as fast as it crosses the board, at every rank,
     * so a slowed Sandling never arrives.
     */
    private static final RankedEnemy SANDLING = RankedEnemy
            .startingAt(EnemyDefinition.of("sandling", "Sandling", 70, 1, 1.5f, BodyArchetype.SANDLING)
                    .withDescription("A handful of the hour's sand, running down the glass. Hold it back and it is gone.")
                    .withMovement(new PathDirectionalMovement())
                    .withAgility(25f)
                    .withIdentifiedTraits(List.of(crumble(4f))), RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalTraits(List.of(crumble(7f))))
            .thenAt(Rank.VETERAN, e -> e
                    .withStats(10f, 10f, 0f, 0f, 45f, 0f, 0f, 10f)
                    .withAdditionalTraits(List.of(crumble(14f))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(25f, 25f, 5f, 5f, 65f, 0f, 0f, 40f)
                    .withAdditionalTraits(List.of(crumble(30f))))
            .thenAt(Rank.BOSS, e -> e
                    .withAdditionalTraits(List.of(crumble(20f)))
                    .withAbilities(List.of(new Ability(AbilityTerm.SPLIT, new OnDeathTrigger(),
                            new SpawnEnemiesAction("sandGrain", AbilitySpawnShape.brood(3, 1f, 0f), false)))))
            .build();

    /**
     * What a Boss Sandling falls apart into: a definition of its own, since a Sandling that spawns Sandlings would be
     * a cycle. It crumbles faster than the Sandling and pays nothing.
     */
    private static final RankedEnemy SAND_GRAIN = RankedEnemy
            .startingAt(EnemyDefinition.of("sandGrain", "Sand Grain", 25, 0, 1.7f, BodyArchetype.SANDLING)
                    .withDescription("A fistful of a Sandling, still running.")
                    .withMovement(new PathDirectionalMovement())
                    .withAgility(25f)
                    .withIdentifiedTraits(List.of(crumble(4f))), RankGrowth.of(12f))
            .thenAt(Rank.BOSS, e -> e.withAdditionalTraits(List.of(crumble(15f))))
            .build();

    /** A walking clock that turns the health of the allies around it back, further with rank, and at Boss its own. */
    private static final RankedEnemy TIMEKEEPER = RankedEnemy
            .startingAt(EnemyDefinition.of("timekeeper", "Timekeeper", 130, 3, 1.0f, BodyArchetype.TIMEKEEPER)
                    .withDescription("Keeps the hour for the others: whatever they lose, it gives back.")
                    .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                    .withIdentifiedAbilities(List.of(rewindAround(50))),
                    RankGrowth.stepped(210, 600, 1300, 2400).paying(4, 10, 26, 40))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalAbilities(List.of(rewindAround(40))))
            .thenAt(Rank.VETERAN, e -> e
                    .withStats(10f, 15f, 0f, 2f, 0f, 15f, 10f, 20f)
                    .withAdditionalAbilities(List.of(rewindAround(50))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(15f, 25f, 0f, 3f, 10f, 25f, 15f, 25f)
                    .withAdditionalAbilities(List.of(rewindAround(60))))
            .thenAt(Rank.BOSS, e -> e
                    .withStats(20f, 30f, 0f, 4f, 10f, 25f, 20f, 30f)
                    .withAdditionalAbilities(List.of(rewindAround(80), IdentifiedAbility.anonymous(new Ability(
                            AbilityTerm.REWIND, new HealthThresholdTrigger(0.25f),
                            new RewindAction(40, new SelfTarget()))))))
            .build();

    /** Glass: thick against every small hit, either type, and shattering under a crit. */
    private static final RankedEnemy GLASS_SENTINEL = RankedEnemy
            .startingAt(EnemyDefinition.of("glassSentinel", "Glass Sentinel", 156, 4, 0.8f, BodyArchetype.GLASS_SENTINEL)
                    .withDescription("Cut from the glass itself. A small hit only rings on it; a hard one shatters it.")
                    .withMovement(new SwingMovement((float) Math.toRadians(6.0), 40, SwingCurve.SMOOTH))
                    .withStats(0f, 0f, 8f, 8f, 0f, -10f, -70f, 5f),
                    RankGrowth.stepped(230, 680, 2578, 4332).paying(5, 9, 24, 40))
            .thenAt(Rank.SOLDIER, e -> e.withPlating(15f).withInsulation(15f))
            .thenAt(Rank.VETERAN, e -> e.withPlating(19f).withInsulation(19f))
            .thenAt(Rank.ELITE, e -> e.withStats(25f, 25f, 23f, 23f, 0f, -20f, -60f, 30f))
            .thenAt(Rank.BOSS, e -> e.withStats(60f, 60f, 25f, 25f, 0f, -40f, -50f, 70f))
            .build();

    /** A brute of packed sand that runs faster as it is hurt and falls apart into Sandlings. */
    private static final RankedEnemy REAVER = RankedEnemy
            .startingAt(EnemyDefinition.of("sandReaver", "Sand Reaver", 110, 6, 1.28f, BodyArchetype.REAVER)
                    .withDescription("Sand packed hard into a brute. It runs faster as it is hurt, and when it falls, "
                            + "it falls apart.")
                    .withMovement(new PathDirectionalMovement())
                    .withHealthDivisor(0.8f)
                    .withPlating(1f)
                    .withInsulation(1f)
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.3f))))
                    .withIdentifiedAbilities(List.of(
                            panicShield(70f),
                            IdentifiedAbility.named("splitOnDeath",
                                    new Ability(AbilityTerm.SPLIT, new OnDeathTrigger(), new SpawnEnemiesAction("sandling", 2, false))))),
                    RankGrowth.stepped(190, 560, 1250, 2100).paying(10, 15, 18, 18))
            .thenAt(Rank.SOLDIER, e -> e
                    .withAdditionalAbilities(List.of(panicShield(126f))))
            .thenAt(Rank.VETERAN, e -> e
                    .withStats(10f, 10f, 2f, 2f, 10f, 0f, 50f, 20f)
                    .withAdditionalAbilities(List.of(panicShield(224f))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(25f, 25f, 6f, 6f, 25f, 0f, 100f, 30f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(new ImmunityTrait(EffectKind.DAZED))))
                    .withAdditionalAbilities(List.of(panicShield(400f))))
            .thenAt(Rank.BOSS, e -> e
                    .withStats(55f, 40f, 10f, 6f, 25f, 0f, 100f, 50f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.9f))))
                    .withAdditionalAbilities(List.of(panicShield(740f))))
            .build();

    /** A shield that holds until broken, once it is badly wounded. */
    private static IdentifiedAbility panicShield(float points) {
        return IdentifiedAbility.named("panicShield", new Ability(AbilityTerm.PANIC, new HealthThresholdTrigger(0.25f),
                new ApplyEffectAction(ShieldTemplate.untilBroken(points), new SelfTarget())));
    }

    private static final QuadraticBezierSmoothing SMOOTHING = new QuadraticBezierSmoothing(0.28, 12);

    /** Into the glass from the top left, down through the waist and out at the bottom. */
    private static final PathColor GLASS_COLOR = PathColor.of(140, 215, 225);
    private static final PathDefinition GLASS_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 1), new Point(7, 1), new Point(7, 5),
                            new Point(5, 7), new Point(2, 9), new Point(2, 15)),
                    List.of(
                            new WaveDefinition("swarm 8 sandling e 4 walker"),
                            new WaveDefinition("column 4 knight e swarm 8 sandling"),
                            new WaveDefinition("timekeeper column 5 walker e swarm 8 sandling").withClearBonus(15),
                            new WaveDefinition("soldier: glassSentinel e column 6 walker").withClearBonus(30),
                            new WaveDefinition("soldier: swarm 12 sandling e timekeeper 4 runner"),
                            new WaveDefinition("soldier: timekeeper 2 glassSentinel e column 6 knight"),
                            new WaveDefinition("veteran: swarm 12 sandling e swarm 12 sandling").withClearBonus(40),
                            new WaveDefinition("veteran: shielded glassSentinel glassSentinel timekeeper e 3 runner").withClearBonus(75),
                            new WaveDefinition("veteran: elite timekeeper column 6 walker e swarm 12 sandling").withClearBonus(60),
                            new WaveDefinition("veteran: drip 6 courier e 2 timekeeper 2 glassSentinel").withClearBonus(100),
                            new WaveDefinition("veteran: swarm 12 sandling e elite timekeeper column 6 knight").withClearBonus(95),
                            new WaveDefinition("veteran: 6 e elite timekeeper swarm 12 sandling e 2 elite glassSentinel")),
                    SMOOTHING)
            .withColor(GLASS_COLOR);

    /** Into the glass from the top, crossing the glass lane at the waist and out to the left. */
    private static final PathColor SAND_COLOR = PathColor.of(235, 195, 110);
    private static final PathDefinition SAND_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(2, -1), new Point(2, 5), new Point(5, 7),
                            new Point(7, 9), new Point(7, 13), new Point(-1, 13)),
                    List.of(
                            new WaveDefinition("6 walker e swarm 6 sandling"),
                            new WaveDefinition("glassSentinel e 4 walker"),
                            new WaveDefinition("swarm 10 sandling e 2 runner"),
                            new WaveDefinition("soldier: timekeeper column 4 walker"),
                            new WaveDefinition("soldier: 3 glassSentinel e swarm 8 sandling"),
                            new WaveDefinition("soldier: drip 4 courier e swarm 12 sandling"),
                            new WaveDefinition("veteran: timekeeper column 6 walker e glassSentinel"),
                            new WaveDefinition("veteran: timekeeper column 6 knight"),
                            new WaveDefinition("veteran: swarm 12 sandling e 2 glassSentinel"),
                            new WaveDefinition("veteran: elite glassSentinel e swarm 12 sandling"),
                            new WaveDefinition("veteran: elite glassSentinel 2 glassSentinel e timekeeper swarm 12 sandling"),
                            new WaveDefinition("veteran: 8 e swarm 12 sandling e elite timekeeper 4 knight")),
                    SMOOTHING)
            .withColor(SAND_COLOR);

    /** Out of the waist itself, a short loop round the left of the glass and back across it. */
    private static final PathColor DUSK_COLOR = PathColor.of(230, 110, 90);
    private static final PathDefinition DUSK_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(9, 5), new Point(2, 5), new Point(0, 7),
                            new Point(2, 9), new Point(9, 9)),
                    List.of(
                            new WaveDefinition("3 sandling"),
                            new WaveDefinition("swarm 6 sandling"),
                            new WaveDefinition("sandReaver"),
                            new WaveDefinition("soldier: swarm 10 sandling e sandReaver"),
                            new WaveDefinition("soldier: 2 sandReaver"),
                            new WaveDefinition("soldier: timekeeper swarm 10 sandling e sandReaver"),
                            new WaveDefinition("veteran: soldier sandReaver e swarm 10 sandling"),
                            new WaveDefinition("veteran: soldier sandReaver timekeeper"),
                            new WaveDefinition("veteran: 2 sandReaver e swarm 12 sandling"),
                            new WaveDefinition("veteran: timekeeper column 4 knight"),
                            new WaveDefinition("veteran: elite sandReaver e swarm 12 sandling"),
                            new WaveDefinition("keeperOfHours e 2 elite timekeeper")),
                    SMOOTHING)
            .withColor(DUSK_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 9, 15, List.of(DUSK_PATH, GLASS_PATH, SAND_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withCardColor(GLASS_COLOR)
            .withGround(BoardGround.of(0x1E2A2C, GroundMotif.DUNES, 0x283739)
                    .withLight(BoardLight.of(0xFFC890, 0x10203A, 0x4E6A70)).withHaze(0.3f))
            // What the Reaver and the Keeper call, before them.
            .withCustomRankedEnemies(List.of(SAND_GRAIN, SANDLING, TIMEKEEPER, GLASS_SENTINEL, REAVER, KeeperEnemy.keeper()));

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
