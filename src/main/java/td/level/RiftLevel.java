package td.level;

import td.board.BoardGround;
import td.board.GroundMotif;
import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;
import td.effect.UnyieldingTemplate;
import td.enemy.Ability;
import td.enemy.AbilitySpawnShape;
import td.enemy.AbilityTerm;
import td.enemy.AdaptiveResist;
import td.enemy.ApplyEffectAction;
import td.enemy.BodyArchetype;
import td.enemy.EffectResistTrait;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.HealthThresholdTrigger;
import td.enemy.IdentifiedAbility;
import td.enemy.IdentifiedTrait;
import td.enemy.LeaveZoneAction;
import td.enemy.OnDodgeTrigger;
import td.enemy.OnFirstDamageTakenTrigger;
import td.enemy.PathDirectionalMovement;
import td.enemy.PeriodicTrigger;
import td.enemy.RadiusTarget;
import td.enemy.Rank;
import td.enemy.RankGrowth;
import td.enemy.RankedEnemy;
import td.enemy.RegenerationTrait;
import td.enemy.RotorMovement;
import td.enemy.SelfTarget;
import td.enemy.SpawnEnemiesAction;
import td.enemy.TimeSinceLastHitTrigger;
import td.wave.PathColor;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;
import td.zone.Zone;

import java.util.List;
import java.util.Optional;

/**
 * The Rift: every lane touches a tear in the middle of the board. Two lanes pour out of it - one spiralling the long
 * way to the edge, one short and quick - and from the middle of the game a third walks back into it. Its enemies
 * each punish one habit: aimed shots, a slow first hit, single targets, and once, one enemy that will not die.
 */
final class RiftLevel {

    private static final String LEVEL_NAME = "The Rift";
    private static final String LEVEL_DESCRIPTION = "Something has torn the board open. Nothing comes from where you "
            + "expect it to.";
    private static final int STARTING_CREDITS = 175;
    private static final int STARTING_LIVES = 3;

    /** What a Mirage leaves where a shot missed it: no bounty, and stronger behind a stronger Mirage. */
    private static final EnemyDefinition ECHO = EnemyDefinition.of("echo", "Mirror Image", 30, 1, 1.6f,
                    BodyArchetype.MIRROR_IMAGE)
            .withDescription("What was left where a shot missed a Mirage.");

    /** A Skirmisher that turns every dodge into one more enemy, so aimed shots feed it and area damage starves it. */
    private static final RankedEnemy MIRAGE = EnemyCatalog.builtIn().ranked("dodger").cloneAs("mirage",
            (rank, dodger) -> dodger
                    .withDisplayName("Mirage")
                    .withArchetype(BodyArchetype.MIRAGE)
                    .withDescription("Nimble, and never quite where the shot went.")
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("mirror", new Ability(AbilityTerm.MIRROR_IMAGE,
                            new OnDodgeTrigger(), new SpawnEnemiesAction("echo",
                                    new AbilitySpawnShape(1, 0.8f, echoHealth(rank), 0f, 0.0, Optional.empty()), false))))));

    /** A Courier whose first wound lays haste ground under everything behind it, so the first hit had better kill. */
    private static final RankedEnemy HERALD = EnemyCatalog.builtIn().ranked("courier").cloneAs("herald",
            courier -> courier
                    .withDisplayName("Herald")
                    .withArchetype(BodyArchetype.HERALD)
                    .withMovement(new PathDirectionalMovement())
                    .withDescription("Runs ahead of the others, and raises the alarm the moment it is hurt.")
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("alarm", new Ability(AbilityTerm.HASTE_FIELD,
                            new OnFirstDamageTakenTrigger(), LeaveZoneAction.hasteGround(1.2f, Zone.UNTIL_WAVE_END))))));

    /** Hides the allies around it from aimed shots every few seconds, but never itself. */
    private static final RankedEnemy ECLIPSE = RankedEnemy
            .startingAt(EnemyDefinition.of("eclipse", "Eclipse", 110, 3, 1.1f, BodyArchetype.ECLIPSE)
                    .withDescription("A shard of the dark inside the rift. Its company keeps vanishing.")
                    .withMovement(new RotorMovement((float) Math.toRadians(4.0)))
                    .withMagicResist(20f)
                    .withIdentifiedAbilities(List.of(eclipse(30))), RankGrowth.of(14f))
            .thenAt(Rank.SOLDIER, e -> e.withAdditionalAbilities(List.of(eclipse(40))))
            .thenAt(Rank.VETERAN, e -> e.withAdditionalAbilities(List.of(eclipse(50))))
            .thenAt(Rank.ELITE, e -> e
                    .withStats(10f, 30f, 0f, 2f, 20f, 10f, 20f)
                    .withAdditionalAbilities(List.of(eclipse(60))))
            .thenAt(Rank.BOSS, e -> e
                    .withStats(20f, 40f, 0f, 4f, 30f, 10f, 30f)
                    .withAdditionalAbilities(List.of(eclipse(80))))
            .build();

    /**
     * The lone walker: slow, crit-proof, hardening against whatever hurts it most, mending whenever it is left alone
     * and hardening twice more on the way down. Letting it through ends the level.
     */
    private static final RankedEnemy MONOLITH = RankedEnemy.unique(
            EnemyDefinition.of("monolith", "The Monolith", 3800, 80, 0.22f, BodyArchetype.MONOLITH)
                    .withDescription("It has walked out of the rift before. It does not hurry, and it does not stop.")
                    .withStats(25f, 25f, 4f, 4f, 100f, 0f, 50f)
                    .withIdentifiedTraits(List.of(
                            IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(50f)),
                            IdentifiedTrait.anonymous(new RegenerationTrait(10f)),
                            IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                            IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.CHILL))))
                    .withIdentifiedAbilities(List.of(
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.RECOVER, new TimeSinceLastHitTrigger(40),
                                    new ApplyEffectAction(new HealTemplate(4f, 40), new SelfTarget()))),
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.STONE_SKIN, new HealthThresholdTrigger(0.66f),
                                    new ApplyEffectAction(ShieldTemplate.untilBroken(600f), new SelfTarget()))),
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.STONE_SKIN, new HealthThresholdTrigger(0.33f),
                                    new ApplyEffectAction(ShieldTemplate.untilBroken(600f), new SelfTarget()))),
                            IdentifiedAbility.anonymous(new Ability(AbilityTerm.LAST_STAND, new HealthThresholdTrigger(0.08f),
                                    new ApplyEffectAction(new UnyieldingTemplate(100), new SelfTarget()))))));

    private static final ArcCornerSmoothing SMOOTHING = new ArcCornerSmoothing(0.3, 10);

    /** Out of the rift the long way: a spiral that passes every ring of the board before it leaves. */
    private static final PathColor VIOLET_COLOR = PathColor.of(160, 90, 230);
    private static final PathDefinition SPIRAL_PATH = PathDefinition.smoothed(
                    List.of(
                            new Point(8, 7), new Point(11, 7), new Point(11, 4), new Point(5, 4), new Point(5, 10),
                            new Point(14, 10), new Point(14, 1), new Point(2, 1), new Point(2, 13), new Point(17, 13)),
                    List.of(
                            new WaveDefinition("6 simple e 2 armored e 4 simple", Rank.GRUNT),
                            new WaveDefinition("4 simple eclipse 4 simple e 2 armored", Rank.GRUNT),
                            new WaveDefinition("column 4 armored e golem e 6 simple", Rank.GRUNT),
                            new WaveDefinition("mirage e 5 simple e mirage e 3 armored", Rank.GRUNT),
                            new WaveDefinition("eclipse column 6 simple e golem e 2 yeti", Rank.SOLDIER),
                            new WaveDefinition("shield column 4 armored e eclipse ghost e 4 simple", Rank.SOLDIER),
                            new WaveDefinition("2 mirage e magma e eclipse column 6 simple e titan", Rank.SOLDIER),
                            new WaveDefinition("e", Rank.GRUNT),
                            new WaveDefinition("eclipse 2 ghost e titan shield e 2 golem e jammer", Rank.VETERAN),
                            new WaveDefinition("line 3 mirage e eclipse column 6 armored e 2 magma", Rank.VETERAN),
                            new WaveDefinition("eclipse column 4 ghost e 2 titan shield e veteran golem e column 10 simple", Rank.SOLDIER),
                            new WaveDefinition("elite eclipse column 6 armored e titan shield e 3 mirage e jammer e "
                                    + "column 10 simple", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(VIOLET_COLOR);

    /** Out of the rift the short way, and quickly. */
    private static final PathColor CYAN_COLOR = PathColor.of(60, 210, 220);
    private static final PathDefinition DASH_PATH = PathDefinition.smoothed(
                    List.of(new Point(8, 7), new Point(-1, 7)),
                    List.of(
                            new WaveDefinition("3 fast", Rank.GRUNT),
                            new WaveDefinition("herald e 3 fast", Rank.GRUNT),
                            new WaveDefinition("swarm 8 mite e herald", Rank.GRUNT),
                            new WaveDefinition("2 herald e 4 fast", Rank.GRUNT),
                            new WaveDefinition("drip 3 herald e 3 fast", Rank.SOLDIER),
                            new WaveDefinition("mirage e swarm 12 mite", Rank.SOLDIER),
                            new WaveDefinition("drip 4 herald e 3 fast e mirage", Rank.SOLDIER),
                            new WaveDefinition("monolith", Rank.BOSS),
                            new WaveDefinition("swarm 12 mite e drip 3 herald", Rank.VETERAN),
                            new WaveDefinition("2 mirage e eclipse 3 fast", Rank.VETERAN),
                            new WaveDefinition("drip 5 herald e veteran fast e swarm 12 mite", Rank.SOLDIER),
                            new WaveDefinition("elite herald e veteran 2 herald e elite fast e 2 mirage", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(CYAN_COLOR)
            .withSpeed(1.1f);

    /** Into the rift: quiet at first, then everything it sends is walking home. */
    private static final PathColor GOLD_COLOR = PathColor.of(235, 190, 60);
    private static final PathDefinition RETURN_PATH = PathDefinition.smoothed(
                    List.of(new Point(8, -1), new Point(8, 7)),
                    List.of(
                            new WaveDefinition("e", Rank.GRUNT),
                            new WaveDefinition("e", Rank.GRUNT),
                            new WaveDefinition("e", Rank.GRUNT),
                            new WaveDefinition("e", Rank.GRUNT),
                            new WaveDefinition("drip 6 simple e 2 armored", Rank.GRUNT),
                            new WaveDefinition("eclipse 3 armored e 2 simple", Rank.SOLDIER),
                            new WaveDefinition("2 yeti e golem", Rank.SOLDIER),
                            new WaveDefinition("e", Rank.GRUNT),
                            new WaveDefinition("3 lizard e magma", Rank.SOLDIER),
                            new WaveDefinition("titan shield e mender 3 armored", Rank.SOLDIER),
                            new WaveDefinition("eclipse 2 ghost e 2 yeti", Rank.VETERAN),
                            new WaveDefinition("elite eclipse column 4 armored e titan", Rank.VETERAN)),
                    SMOOTHING)
            .withColor(GOLD_COLOR)
            .withSpeed(0.85f);

    static final LevelDefinition DEFINITION = LevelDefinition
            .of(LEVEL_NAME, 17, 15, List.of(SPIRAL_PATH, DASH_PATH, RETURN_PATH))
            .withDescription(LEVEL_DESCRIPTION)
            .withStartingCredits(STARTING_CREDITS)
            .withStartingLives(STARTING_LIVES)
            .withGround(BoardGround.of(0x07040C, GroundMotif.FISSURES, 0x180E25))
            .withCustomEnemies(List.of(ECHO))
            .withCustomRankedEnemies(List.of(MIRAGE, HERALD, ECLIPSE, MONOLITH));

    private RiftLevel() {
    }

    /** The health an echo spawns with, as a multiple of its own, behind a Mirage of {@code rank}. */
    private static float echoHealth(Rank rank) {
        return switch (rank) {
            case GRUNT -> 1f;
            case SOLDIER -> 2f;
            case VETERAN -> 3.5f;
            case ELITE -> 6f;
            case BOSS -> 10f;
        };
    }

    /** Every few seconds, the allies around it vanish from aimed shots for {@code ticks}. */
    private static IdentifiedAbility eclipse(int ticks) {
        return IdentifiedAbility.named("eclipse", new Ability(AbilityTerm.SHROUD, new PeriodicTrigger(100),
                new ApplyEffectAction(new InvisibleTemplate(ticks), new RadiusTarget(110f))));
    }
}
