package td.level;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.enemy.Ability;
import td.enemy.AbilitySpawnShape;
import td.enemy.AbilityTerm;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.IdentifiedAbility;
import td.enemy.LeaveZoneAction;
import td.enemy.OnDeathTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.PeriodicTrigger;
import td.enemy.PulseMovement;
import td.enemy.Rank;
import td.enemy.RankGrowth;
import td.enemy.RankedEnemy;
import td.enemy.RotorMovement;
import td.enemy.ShoveAlliesAction;
import td.enemy.SpawnEnemiesAction;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;
import java.util.stream.Stream;

/**
 * The Storm Ridge: two lanes cross a ridge as a storm breaks. Gales shove whatever walks beside them further along,
 * so a defence has to hold the length of a lane rather than one corner of it; clouds burst into rain; and at the end
 * the storm's twins, one down each lane, who have to fall close together.
 */
final class StormRidgeLevel {

    private static final String LEVEL_NAME = "The Storm Ridge";
    private static final String LEVEL_DESCRIPTION = "Two lanes cross the ridge as the storm breaks. Lightning strikes "
            + "and thunder follows - and whichever falls first, the other calls back.";
    private static final int STARTING_CREDITS = 90;
    private static final int STARTING_LIVES = 4;
    private static final int CLEAR_BONUS = 4;

    /** What a cloud bursts into: tiny, quick and a little nimble. */
    private static final EnemyDefinition RAINDROP = EnemyDefinition.of("raindrop", "Raindrop", 30, 1, 2.0f, BodyArchetype.RAINDROP)
            .withDescription("One drop of a burst cloud. There are always more.")
            .withMovement(new PathDirectionalMovement())
            .withAgility(20f);

    /** A slow cloud that bursts into rain when it dies, and from Elite leaves rain behind it that heals. */
    private static final RankedEnemy RAINCLOUD = RankedEnemy
            .startingAt(EnemyDefinition.of("raincloud", "Raincloud", 170, 3, 0.9f, BodyArchetype.RAINCLOUD)
                    .withDescription("Heavy with rain. Burst it, and the rain still comes down the lane.")
                    .withMovement(new PulseMovement(0.1f, 30, 10))
                    .withMagicResist(15f)
                    .withIdentifiedAbilities(List.of(downpour(3))), RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalAbilities(List.of(downpour(4))))
            .thenAt(Rank.VETERAN, e -> e.withAdditionalAbilities(List.of(downpour(5))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(0f, 25f, 0f, 1f, 0f, 10f, 20f)
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("rain", new Ability(AbilityTerm.HEALING_POOL,
                            new PeriodicTrigger(60), LeaveZoneAction.healingGround(0.8f, 80, 0.3f))))))
            .thenAt(Rank.BOSS, e -> e.withAdditionalAbilities(List.of(downpour(8))))
            .build();

    /** A whirlwind that shoves the enemies beside it further along the lane, harder with rank. */
    private static final RankedEnemy GALE = RankedEnemy
            .startingAt(EnemyDefinition.of("gale", "Gale", 120, 3, 1.4f, BodyArchetype.GALE)
                    .withDescription("The wind at the storm's back. Whatever walks beside it walks faster than it should.")
                    .withMovement(new RotorMovement((float) Math.toRadians(9.0)))
                    .withAgility(15f)
                    .withIdentifiedAbilities(List.of(gust(1.5f, 1.2f))), RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalAbilities(List.of(gust(1.6f, 1.5f))))
            .thenAt(Rank.VETERAN, e -> e.withAdditionalAbilities(List.of(gust(1.8f, 1.8f))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(5f, 15f, 0f, 0f, 0f, 30f, 10f)
                    .withAdditionalAbilities(List.of(gust(2.0f, 2.2f))))
            .thenAt(Rank.BOSS, e -> e.withAdditionalAbilities(List.of(gust(2.4f, 3.0f))))
            .build();

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.4, 10);

    /** The long road, back and forth across the ridge: where the heavy half of the storm walks. */
    private static final PathColor THUNDER_COLOR = PathColor.of(95, 115, 235);
    private static final PathDefinition THUNDER_ROAD = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 2), new Point(13, 2), new Point(13, 5), new Point(2, 5),
                            new Point(2, 8), new Point(16, 8)),
                    List.of(
                            new WaveDefinition("6 raincloud e 4 walker").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("column 4 knight e 2 raincloud e 4 walker").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("gale column 5 walker 2 e gale column 5 walker")
                                    .withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("soldier: 3 raincloud e golem e 3 raincloud").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("soldier: gale column 6 knight e 2 raincloud").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("soldier: jammer 4 raincloud e gale column 6 walker").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: 2 golem e gale column 6 knight e 3 raincloud").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("w30 veteran: 6 raincloud e 2 gale column 6 walker").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: juggernaut gale e column 6 knight e 4 raincloud").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: elite hulking raincloud 3 raincloud e 2 gale column 8 knight")
                                    .withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: elite gale column 8 knight e elite jammer 4 raincloud e golem")
                                    .withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("elite: thunder e column 6 knight e elite gale e 4 raincloud")
                                    .withClearBonus(CLEAR_BONUS)),
                    SMOOTHING)
            .withColor(THUNDER_COLOR);

    /** The short lane, cutting the road twice: where the quick half of the storm strikes. */
    private static final PathColor LIGHTNING_COLOR = PathColor.of(255, 225, 70);
    private static final PathDefinition LIGHTNING_LANE = PathDefinition.smoothed(
                    List.of(
                            new Point(12, -1), new Point(12, 2), new Point(9, 10), new Point(7, 10),
                            new Point(3, 2), new Point(3, -1)),
                    List.of(
                            new WaveDefinition("3 runner e swarm 6 raindrop").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("drip 3 courier e 3 runner").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("swarm 8 raindrop e gale 3 runner").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("soldier: drip 4 courier e raincloud").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("soldier: line 3 skirmisher e swarm 10 raindrop").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("soldier: gale 4 runner e drip 3 courier").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: swarm 12 raindrop e gale e swarm 12 raindrop").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: line 3 skirmisher e 2 raincloud e drip 4 courier").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: gale 4 runner e gale 4 elusive runner").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: drip 5 courier e swarm 12 raindrop e line 3 skirmisher")
                                    .withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("veteran: elite runner e gale 4 courier e swarm 12 mite").withClearBonus(CLEAR_BONUS),
                            new WaveDefinition("elite: 24 e lightning e swarm 10 raindrop").withClearBonus(CLEAR_BONUS)),
                    SMOOTHING)
            .withColor(LIGHTNING_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 16, 11, List.of(THUNDER_ROAD, LIGHTNING_LANE))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withGround(BoardGround.of(0x06090E, GroundMotif.RAIN, 0x121A2A))
            .withCustomEnemies(List.of(RAINDROP))
            .withCustomRankedEnemies(Stream.concat(Stream.of(RAINCLOUD, GALE), StormTwinsEnemy.pair().stream()).toList());

    private StormRidgeLevel() {
    }

    /** When it dies, it bursts into {@code drops} raindrops. */
    private static IdentifiedAbility downpour(int drops) {
        return IdentifiedAbility.named("downpour", new Ability(AbilityTerm.BROOD, new OnDeathTrigger(),
                new SpawnEnemiesAction("raindrop", AbilitySpawnShape.brood(drops, 1f, 1f), false)));
    }

    /** Every four seconds, shoves the enemies within {@code reachCells} {@code distanceCells} further along. */
    private static IdentifiedAbility gust(float reachCells, float distanceCells) {
        return IdentifiedAbility.named("gust", new Ability(AbilityTerm.GUST, new PeriodicTrigger(80),
                new ShoveAlliesAction(reachCells, distanceCells)));
    }
}
