package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;
import td.stat.DisruptionAura;

import java.util.ArrayList;
import java.util.List;

/**
 * The built-in rank ladders and standalone definitions that {@link EnemyCatalog#builtIn()}
 * registers under their wave-script ids.
 * <p>
 * A definition is named for what it does; its {@link BodyArchetype} names the shape it is drawn as.
 * <p>
 * Name a trait or ability ({@link IdentifiedTrait#named}, {@link IdentifiedAbility#named}) only
 * when a later rank step, a clone or a spawn shape will replace it by id; otherwise leave it
 * anonymous.
 * <p>
 * The boss chain is a finite, linear sequence of single-rank definitions: each boss stage lays an
 * egg on death, each egg hatches into the next, weaker stage unless killed, and the last egg has no
 * ability, so the chain always ends.
 */
final class BuiltInEnemies {

    private static final AdaptiveResist ELITE_ARMOR = AdaptiveResist.againstDominant(0.6f);

    // Bounty grows 2.5x per rank while health only doubles, so tougher mobs pay
    // disproportionately more.
    static final RankedEnemy SIMPLE = RankedEnemy
            .startingAt(EnemyDefinition.of("c", "Simple mob", 50, 2, 1.28f, BodyArchetype.CIRCLE)
                    .withDescription("No special abilities."))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(100, 5))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(200, 13))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(400, 33)
                    .withDescription("No special abilities, but a layer of armor has formed, "
                            + "hardened against whichever damage type has hit hardest this level.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(ELITE_ARMOR))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(800, 83)
                    .withDescription("No special abilities, but twice an elite's bulk under the same armor, hardened "
                            + "against whichever damage type has hit hardest this level."))
            .build();

    static final RankedEnemy ARMORED = RankedEnemy
            .startingAt(EnemyDefinition.of("s", "Armored mob", 80, 3, 1.28f, BodyArchetype.SQUARE)
                    .withDescription("Takes less damage. Immune to critical hits.")
                    .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("resist", new PercentResistTrait(0.8f)),
                            IdentifiedTrait.named("criticalImmune", new CriticalImmunityTrait()))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(160, 8)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("resist", new PercentResistTrait(0.75f)))))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(320, 20)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("resist", new PercentResistTrait(0.7f)))))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(640, 50)
                    .withDescription("Takes far less damage, and its plating has grown thick enough to blunt a flat "
                            + "chunk of every hit outright. Immune to critical hits.")
                    .withAdditionalTraits(List.of(
                            IdentifiedTrait.named("flatResist", FlatResistTrait.physicalOnly(8)),
                            IdentifiedTrait.named("resist", new PercentResistTrait(0.6f)))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1280, 125)
                    .withDescription("Takes drastically less damage, and its plating blunts a large flat chunk of "
                            + "every hit outright. Immune to critical hits.")
                    .withAdditionalTraits(List.of(
                            IdentifiedTrait.named("flatResist", FlatResistTrait.physicalOnly(10)),
                            IdentifiedTrait.named("resist", new PercentResistTrait(0.5f)))))
            .build();

    private static final float JAMMER_RADIUS = 100f;
    private static final float JAMMER_FIRE_RATE_PENALTY = 0.3f;
    private static final float JAMMER_RANGE_PENALTY = 0.2f;
    static final EnemyDefinition JAMMER = EnemyDefinition.of("j", "Jammer", 120, 6, 1.0f, BodyArchetype.SQUARE)
            .withDescription("Jams every tower near it: they fire more slowly and see less far while it's in range.")
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withDisruption(new DisruptionAura(JAMMER_RADIUS, JAMMER_FIRE_RATE_PENALTY, JAMMER_RANGE_PENALTY));

    private static final float FRENZIED_SPAWN_HEALTH_THRESHOLD = 0.5f;
    private static final int FRENZIED_SPAWN_COUNT = 3;
    private static final int FRENZIED_NEGLECT_WINDOW_TICKS = 200;
    private static final float FRENZIED_NEGLECT_HEAL_PER_TICK = 0.02f;
    private static final int FRENZIED_NEGLECT_HEAL_DURATION_TICKS = 200;
    static final RankedEnemy FRENZIED = RankedEnemy
            .startingAt(EnemyDefinition.of("t", "Frenzied mob", 60, 3, 1.28f, BodyArchetype.TRIANGLE)
                    .withDescription("Increases speed as it takes damage.")
                    .withMovement(new RotorMovement((float) Math.toRadians(-5.0)))
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.4f)))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(120, 8))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(240, 20)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.5f)))))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(480, 50)
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.8f)))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(960, 125)
                    .withDescription("Increases speed as it takes damage, now immune to critical hits, calls a "
                            + "swarm of reinforcements once badly hurt, and heals itself if left unattacked too long.")
                    .withAdditionalTraits(List.of(
                            IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(2f)),
                            IdentifiedTrait.anonymous(new CriticalImmunityTrait())))
                    .withMovement(new RotorMovement((float) Math.toRadians(-10.0)))
                    .withAdditionalAbilities(List.of(
                            IdentifiedAbility.anonymous(new Ability(new HealthThresholdTrigger(FRENZIED_SPAWN_HEALTH_THRESHOLD),
                                    new SpawnEnemiesAction("tSpawn", AbilitySpawnShape.brood(FRENZIED_SPAWN_COUNT, 1f, 1f), false))),
                            IdentifiedAbility.anonymous(new Ability(new TimeSinceLastHitTrigger(FRENZIED_NEGLECT_WINDOW_TICKS),
                                    new ApplyEffectAction(new HealTemplate(FRENZIED_NEGLECT_HEAL_PER_TICK, FRENZIED_NEGLECT_HEAL_DURATION_TICKS),
                                            new SelfTarget()))))))
            .build();
    private static final float T_SPAWN_DEATH_HEAL_PER_TICK = 0.04f;
    private static final int T_SPAWN_DEATH_HEAL_DURATION_TICKS = 40;
    private static final float T_SPAWN_DEATH_HEAL_RADIUS = 150f;
    static final EnemyDefinition T_SPAWN = EnemyDefinition
            .of("tSpawn", "Frenzy Spawnling", 140, 2, 1.28f, BodyArchetype.TRIANGLE)
            .withDescription("A spawnling of an enraged Frenzied boss. Speeds up as it is hurt, and heals nearby "
                    + "allies with its dying breath.")
            .withTraits(List.of(new HurtSpeedTrait(1.4f)))
            .withAbilities(List.of(new Ability(
                    new OnDeathTrigger(),
                    new ApplyEffectAction(new HealTemplate(T_SPAWN_DEATH_HEAL_PER_TICK, T_SPAWN_DEATH_HEAL_DURATION_TICKS),
                            new RadiusTarget(T_SPAWN_DEATH_HEAL_RADIUS)))));
    private static final int GHOST_VANISH_DURATION_TICKS = 200;
    // Each application outlasts the interval, so an ally in radius never flickers visible.
    private static final int GHOST_SHROUD_INTERVAL_TICKS = 20;
    private static final int GHOST_SHROUD_DURATION_TICKS = 40;
    private static final float GHOST_SHROUD_RADIUS = 100f;
    static final RankedEnemy GHOST = RankedEnemy
            .startingAt(EnemyDefinition.of("g", "Ghost mob", 100, 4, 1.28f, BodyArchetype.GHOST)
                    .withDescription("An ordinary mob that turns invisible to towers for a while "
                            + "the first time it's hit. Area damage still finds it.")
                    .withAbilities(List.of(new Ability(
                            // A hit that also freezes suppresses this cast outright: a frozen mob cannot cast.
                            new OnFirstDamageTakenTrigger(),
                            new ApplyEffectAction(new InvisibleTemplate(GHOST_VANISH_DURATION_TICKS), new SelfTarget())))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(200, 10))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(400, 25))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(800, 63)
                    .withDescription("Turns invisible to towers for a while the first time it's "
                            + "hit, and permanently shrouds every other ally near it - itself excluded.")
                    .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                            new PeriodicTrigger(GHOST_SHROUD_INTERVAL_TICKS),
                            new ApplyEffectAction(new InvisibleTemplate(GHOST_SHROUD_DURATION_TICKS), new RadiusTarget(GHOST_SHROUD_RADIUS)))))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1600, 158))
            .build();
    private static final int MENDER_HEAL_INTERVAL_TICKS = 20;
    private static final int MENDER_HEAL_DURATION_TICKS = 40;
    private static final float MENDER_HEAL_PER_TICK = 0.02f;
    private static final float MENDER_HEAL_RADIUS = 80f;
    private static final int MENDER_SELF_HEAL_WINDOW_TICKS = 100;
    private static final int MENDER_SELF_HEAL_DURATION_TICKS = 100;
    static final RankedEnemy MENDER = RankedEnemy
            .startingAt(EnemyDefinition.of("m", "Mender mob", 60, 3, 1.28f, BodyArchetype.MENDER)
                    // "other" is load-bearing: a radius target excludes the caster.
                    .withDescription("Periodically restores health to every other ally near it - itself excluded.")
                    .withIdentifiedAbilities(List.of(IdentifiedAbility.named("heal", new Ability(
                            new PeriodicTrigger(MENDER_HEAL_INTERVAL_TICKS),
                            new ApplyEffectAction(new HealTemplate(MENDER_HEAL_PER_TICK, MENDER_HEAL_DURATION_TICKS),
                                    new RadiusTarget(MENDER_HEAL_RADIUS)))))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(120, 8))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(240, 20)
                    .withDescription("Restores health to every other ally near it - itself excluded - and now "
                            + "quietly mends itself if left unattacked long enough.")
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("selfHeal", new Ability(
                            new TimeSinceLastHitTrigger(MENDER_SELF_HEAL_WINDOW_TICKS),
                            new ApplyEffectAction(new HealTemplate(0.01f, MENDER_SELF_HEAL_DURATION_TICKS), new SelfTarget()))))))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(480, 50)
                    .withDescription("Its healing pulse now lasts twice as long, and a personal layer of armor, "
                            + "hardened against whichever damage type has hit hardest this level, joins its own "
                            + "quiet self-repair when left unattacked.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(ELITE_ARMOR)))
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("heal", new Ability(
                            new PeriodicTrigger(MENDER_HEAL_INTERVAL_TICKS),
                            new ApplyEffectAction(new HealTemplate(MENDER_HEAL_PER_TICK, MENDER_HEAL_DURATION_TICKS * 2),
                                    new RadiusTarget(MENDER_HEAL_RADIUS)))))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(960, 125)
                    .withDescription("Its healing pulse now restores double, for the same extended duration, and "
                            + "its own self-repair heals twice as strong too.")
                    .withAdditionalAbilities(List.of(
                            IdentifiedAbility.named("heal", new Ability(
                                    new PeriodicTrigger(MENDER_HEAL_INTERVAL_TICKS),
                                    new ApplyEffectAction(new HealTemplate(MENDER_HEAL_PER_TICK * 2, MENDER_HEAL_DURATION_TICKS * 2),
                                            new RadiusTarget(MENDER_HEAL_RADIUS)))),
                            IdentifiedAbility.named("selfHeal", new Ability(
                                    new TimeSinceLastHitTrigger(MENDER_SELF_HEAL_WINDOW_TICKS),
                                    new ApplyEffectAction(new HealTemplate(0.02f, MENDER_SELF_HEAL_DURATION_TICKS), new SelfTarget()))))))
            .build();
    static final EnemyDefinition WARDEN_EGG_3 = EnemyDefinition
            .of("wardenEgg3", "Warden's Final Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Must be defeated to end the encounter - it will not hatch again.");
    private static final int EGG_HATCH_DELAY_TICKS = 160;
    static final EnemyDefinition WARDEN_EGG_1 = EnemyDefinition
            .of("wardenEgg1", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time. Immune to critical hits, and "
                    + "its armor blunts part of every hit.")
            .withIdentifiedTraits(List.of(
                    IdentifiedTrait.anonymous(new CriticalImmunityTrait()),
                    IdentifiedTrait.named("armor", new PercentResistTrait(0.6f))))
            .withAbilities(List.of(new Ability(
                    new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden2", 1, true))));
    static final EnemyDefinition WARDEN_EGG_2 = EnemyDefinition
            .of("wardenEgg2", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time. Immune to burn and freeze, "
                    + "and its plating blunts a flat chunk of every hit.")
            .withIdentifiedTraits(List.of(
                    IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                    IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                    IdentifiedTrait.named("armor", new FlatResistTrait(1))))
            .withAbilities(List.of(new Ability(
                    new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden3", 1, true))));
    // High enough that an un-upgraded weak tower does nothing; later stages weaken it.
    private static final float WARDEN_FLAT_RESIST = 10f;
    private static final String WARDEN_ABILITY_BLURB = " Periodically calls a reinforcement and re-shields itself; "
            + "shields every nearby ally once below half health; calls an extra reinforcement if left unattacked "
            + "too long; gains a shield whenever it survives a critical hit; and leaves behind an egg on death.";
    private static final List<IdentifiedAbility> WARDEN_STANDING_ABILITIES = List.of(
            IdentifiedAbility.anonymous(new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false))),
            IdentifiedAbility.anonymous(new Ability(new PeriodicTrigger(400),
                    new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget()))),
            IdentifiedAbility.anonymous(new Ability(new HealthThresholdTrigger(0.5f),
                    new ApplyEffectAction(new ShieldTemplate(0.3f, 150), new RadiusTarget(150f)))),
            // Re-arms after each hit, so it fires every time it is left alone, not just once.
            IdentifiedAbility.anonymous(new Ability(new TimeSinceLastHitTrigger(200),
                    new ApplyEffectAction(new HealTemplate(0.04f, 40), new SelfTarget()))),
            IdentifiedAbility.anonymous(new Ability(new OnCriticalHitTakenTrigger(),
                    new ApplyEffectAction(new ShieldTemplate(0.3f, 30), new SelfTarget()))));
    static final EnemyDefinition WARDEN_1 = EnemyDefinition
            .of("warden1", "The Warden", 8000, 100, 0.8f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", FlatResistTrait.physicalOnly(WARDEN_FLAT_RESIST))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg1"));
    static final EnemyDefinition WARDEN_2 = EnemyDefinition
            .of("warden2", "The Weakened Warden", 5000, 100, 0.8f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, worn down from its last hatching." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", FlatResistTrait.physicalOnly(WARDEN_FLAT_RESIST / 2))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg2"));
    static final EnemyDefinition WARDEN_3 = EnemyDefinition
            .of("warden3", "The Exhausted Warden", 3000, 100, 0.8f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, barely standing." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", FlatResistTrait.physicalOnly(WARDEN_FLAT_RESIST / 4))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg3"));

    private BuiltInEnemies() {
    }

    private static List<IdentifiedAbility> wardenAbilities(String eggId) {
        List<IdentifiedAbility> abilities = new ArrayList<>(WARDEN_STANDING_ABILITIES);
        abilities.add(IdentifiedAbility.anonymous(new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(eggId, 1, false))));
        return List.copyOf(abilities);
    }
}
