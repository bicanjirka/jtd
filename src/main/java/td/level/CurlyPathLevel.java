package td.level;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.effect.ShieldTemplate;
import td.effect.SprintTemplate;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.FixedMovement;
import td.enemy.HivemindTrait;
import td.enemy.HurtSpeedTrait;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.OnDodgeTrigger;
import td.enemy.OnceTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.PeriodicTrigger;
import td.enemy.PulseMovement;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.RankGrowth;
import td.enemy.RankedEnemy;
import td.enemy.SelfTarget;
import td.enemy.SpawnEnemiesAction;
import td.enemy.StrideMovement;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.List;
import java.util.stream.Stream;

/**
 * The Amber Hive: one lane coiling through a comb, and a brood that is only strong together. Its workers and guards
 * harden for every ally beside them, so a defence has to break the pack apart; its larvae spin cocoons and hatch into
 * hornets if they are let walk; and at the end the Warden, who calls the brood to its side.
 */
final class CurlyPathLevel {

    private static final String LEVEL_NAME = "The Amber Hive";
    private static final String LEVEL_DESCRIPTION = "A trail coils through a comb of amber cells. The brood marches "
            + "together - pick it apart, or it will not fall. The Warden walks last.";
    private static final int STARTING_CREDITS = 60;
    private static final int STARTING_LIVES = 5;

    /** A Simple mob that never walks alone: tougher for every ally beside it. */
    private static final RankedEnemy DRONE = EnemyCatalog.builtIn().ranked("simple").cloneAs("drone",
            simple -> simple
                    .withDisplayName("Drone")
                    .withArchetype(BodyArchetype.DRONE)
                    .withDescription("A worker of the hive. Never alone, and never as weak as it looks.")
                    .withMovement(new StrideMovement((float) Math.toRadians(16.0), 4))
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hivemind", new HivemindTrait(4f, 5)))));

    /** A slow, thick-shelled drone that hardens far more in company. */
    private static final RankedEnemy HIVE_GUARD = EnemyCatalog.builtIn().ranked("simple").cloneAs("hiveguard",
            simple -> simple
                    .withDisplayName("Hive Guard")
                    .withArchetype(BodyArchetype.HIVE_GUARD)
                    .withDescription("A beetle in amber chitin. Alone it is slow; in a crowd it is a wall.")
                    .withMovement(new StrideMovement((float) Math.toRadians(5.0), 20))
                    .withHealthAndPrice(simple.baseHealth() * 3, simple.price() * 2)
                    .withBaseSpeed(simple.baseSpeed() / 2f)
                    .withPlating(1f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hivemind", new HivemindTrait(9f, 4)))));

    /**
     * The hive's Menders: from Veteran they also ward the allies near them with a shield that takes every damage type,
     * so the answer is to deny heals and shields, or to reach the nurse first.
     */
    private static final RankedEnemy NURSE = EnemyCatalog.builtIn().ranked("mender").cloneAs("nurse",
            (rank, mender) -> {
                EnemyDefinition nurse = mender
                        .withDisplayName("Brood Nurse")
                        .withArchetype(BodyArchetype.NURSE)
                        .withDescription("Tends the brood on the march: heals it, and from Veteran wards it.");
                return switch (rank) {
                    case GRUNT, SOLDIER -> nurse;
                    case VETERAN -> warding(nurse, 25f);
                    case ELITE -> warding(nurse, 40f);
                    case BOSS -> warding(nurse, 70f);
                };
            });

    /** What a cocoon breaks open into: quick and nimble, faster still once it is hurt. */
    private static final RankedEnemy HORNET = RankedEnemy
            .startingAt(EnemyDefinition.of("hornet", "Hornet", 110, 4, 2.0f, BodyArchetype.HORNET)
                    .withDescription("What the cocoon was keeping. It does not walk the coil; it flies it.")
                    .withMovement(new PathDirectionalMovement())
                    .withAgility(25f)
                    .withResilience(-20f)
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.3f)))),
                    RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAgility(30f))
            .thenAt(Rank.VETERAN, e -> e
                    .withAgility(35f)
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("sidestep", new Ability(AbilityTerm.SIDESTEP,
                            new OnDodgeTrigger(), new ApplyEffectAction(new SprintTemplate(20), new SelfTarget()))))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(10f, 10f, 0f, 0f, -20f, 45f, 10f)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.6f)))))
            .build();

    /** A larva left to walk long enough: still, armored and soft to a crit, and hatching unless it is broken open. */
    private static final RankedEnemy COCOON = RankedEnemy
            .startingAt(EnemyDefinition.of("cocoon", "Cocoon", 240, 3, 0f, BodyArchetype.COCOON)
                    .withDescription("Silk wound round something that is still changing. It will not be still for long.")
                    .withMovement(new FixedMovement())
                    .withStats(30f, 0f, 2f, 0f, -40f, 0f, 0f)
                    .withIdentifiedAbilities(List.of(IdentifiedAbility.named("hatch", new Ability(AbilityTerm.HATCH,
                            new OnceTrigger(120), new SpawnEnemiesAction("hornet", 1, true))))),
                    RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withArmor(35f))
            .thenAt(Rank.VETERAN, e -> e.withStats(40f, 10f, 3f, 0f, -40f, 0f, 0f))
            .thenAt(Rank.ELITE, e -> e.withStats(50f, 25f, 4f, 2f, -40f, 0f, 20f))
            .build();

    /** A soft grub that crawls a while, then spins a cocoon where it stands. */
    private static final RankedEnemy LARVA = EnemyCatalog.builtIn().ranked("simple").cloneAs("larva",
            simple -> simple
                    .withDisplayName("Larva")
                    .withArchetype(BodyArchetype.LARVA)
                    .withDescription("Soft, slow and nothing to fear - if it dies before it spins.")
                    .withMovement(new PulseMovement(0.15f, 14, 6))
                    .withHealthAndPrice(Math.round(simple.baseHealth() * 0.8f), simple.price())
                    .withBaseSpeed(simple.baseSpeed() * 0.7f)
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("pupate", new Ability(AbilityTerm.PUPATE,
                            new OnceTrigger(180), new SpawnEnemiesAction("cocoon", 1, true))))));

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.22, 8);

    private static final PathColor HONEY_COLOR = PathColor.of(240, 175, 40);
    private static final PathDefinition COIL_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(-1, 10), new Point(2, 10), new Point(3, 9), new Point(3, 2),
                            new Point(2, 1), new Point(1, 2), new Point(1, 9), new Point(2, 10),
                            new Point(11, 10), new Point(13, 11), new Point(15, 11), new Point(17, 10),
                            new Point(18, 8), new Point(18, 6), new Point(17, 4), new Point(15, 3),
                            new Point(13, 3), new Point(11, 4), new Point(10, 6), new Point(10, 8),
                            new Point(6, 8), new Point(6, 3), new Point(7, 2), new Point(11, 2), new Point(11, -1)),
                    List.of(
                            new WaveDefinition("column 8 drone 2 e column 6 drone", Rank.GRUNT).withClearBonus(5),
                            new WaveDefinition("3 larva 2 e column 6 drone e 3 larva", Rank.GRUNT).withClearBonus(5),
                            new WaveDefinition("column 6 drone e hiveguard e column 6 drone e hiveguard", Rank.GRUNT)
                                    .withClearBonus(5),
                            new WaveDefinition("nurse column 6 drone 2 e nurse column 6 drone e 2 hiveguard", Rank.SOLDIER)
                                    .withClearBonus(5),
                            new WaveDefinition("drip 6 larva e swarm 12 mite e drip 4 larva", Rank.SOLDIER).withClearBonus(5),
                            new WaveDefinition("2 hiveguard nurse 2 hiveguard 2 e column 8 drone", Rank.SOLDIER)
                                    .withClearBonus(5),
                            new WaveDefinition("line 3 hornet 2 e column 8 drone 2 e line 3 hornet", Rank.SOLDIER)
                                    .withClearBonus(5),
                            new WaveDefinition("w30 6 larva e 3 hiveguard nurse e 6 larva", Rank.VETERAN).withClearBonus(5),
                            new WaveDefinition("swarm 12 mite e column 10 drone e swarm 12 mite e veteran nurse column 10 drone",
                                    Rank.SOLDIER).withClearBonus(5),
                            new WaveDefinition("elite hiveguard 2 nurse e column 8 drone e 4 hornet", Rank.VETERAN)
                                    .withClearBonus(5),
                            new WaveDefinition("drip 8 larva e elite nurse column 12 drone e drip 6 larva", Rank.VETERAN)
                                    .withClearBonus(5),
                            new WaveDefinition("column 12 drone e 2 elite hiveguard elite nurse e column 12 drone e 6 hornet",
                                    Rank.VETERAN).withClearBonus(5),
                            new WaveDefinition("line 4 hornet e line 4 hornet e elite hornet e swarm 12 mite e line 4 hornet",
                                    Rank.VETERAN).withClearBonus(5),
                            new WaveDefinition("column 3 hiveguard 2 nurse e column 12 drone e drip 8 larva", Rank.ELITE)
                                    .withClearBonus(5),
                            new WaveDefinition("column 12 drone e boss nurse 2 hiveguard e 8 hornet e drip 8 larva", Rank.ELITE)
                                    .withClearBonus(5),
                            new WaveDefinition("warden 8 e elite column 10 drone e elite nurse e column 10 drone", Rank.BOSS)
                                    .withClearBonus(5)),
                    SMOOTHING)
            .withColor(HONEY_COLOR);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 20, 13, List.of(COIL_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withGround(BoardGround.of(0x100B04, GroundMotif.COMB, 0x1C1408))
            // The drone first, so the Warden's reinforcements name a registered id; each hatch's target before it.
            .withCustomRankedEnemies(Stream.concat(Stream.of(DRONE, HIVE_GUARD, NURSE, HORNET, COCOON, LARVA),
                    WardenEnemy.chain().stream()).toList());

    private CurlyPathLevel() {
    }

    private static EnemyDefinition warding(EnemyDefinition nurse, float points) {
        return nurse.withAdditionalAbilities(List.of(IdentifiedAbility.named("ward", new Ability(AbilityTerm.WARD,
                new PeriodicTrigger(100), new ApplyEffectAction(new ShieldTemplate(points, 60), new RadiusTarget(80f))))));
    }
}
