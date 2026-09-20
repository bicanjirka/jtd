package td.enemy;

import td.effect.HealTemplate;
import td.effect.InvisibleTemplate;
import td.effect.ShieldTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The built-in {@link RankedEnemy} ladders and standalone {@link EnemyDefinition}s.
 * {@link EnemyCatalog#builtIn()} pre-registers all of these under their wave-script ids. Two
 * groups:
 * <p>
 * {@code SIMPLE}/{@code ARMORED}/{@code FRENZIED}/{@code GHOST}/{@code MENDER} are the five basic
 * mobs, each a full five-rank ladder - every rank above {@link Rank#GRUNT} simply doubles the
 * rank before it's health and scales its bounty to match, {@code withHealthAndPrice} being the
 * only thing each step changes, except {@code SIMPLE} itself: its own ladder is this feature's
 * demonstration that a later rank can both add a trait (at {@link Rank#ELITE}) and replace it
 * with a stronger one (at {@link Rank#BOSS}) via the same identified-trait mechanism, following
 * the shape of the request's own pseudocode; {@code GHOST}'s own ladder is the ability-driven
 * counterpart - it carries no native invisibility of any kind, only an ordinary mob with a
 * "vanish on first hit" ability at every rank, plus a second, radius-targeted "shroud nearby
 * allies" ability added at {@link Rank#ELITE} (and, like {@code SIMPLE}'s armor, carried forward
 * unchanged into {@link Rank#BOSS} since that step only touches health and price).
 * {@code MENDER} carries the same radius-targeted shape as {@code GHOST}'s shroud, just with
 * {@link td.effect.HealTemplate} in place of {@link InvisibleTemplate}, and - unlike the shroud -
 * present at every rank rather than added at Elite, since periodically healing nearby allies is
 * this mob's whole reason to exist rather than an upgrade over a baseline behaviour.
 * <strong>A definition is named for what it does; its
 * {@link BodyArchetype} is what names the shape it is drawn as</strong> - so {@code ARMORED} is
 * a square and {@code FRENZIED} a triangle, the same way {@code SniperTower} is drawn as a
 * triangle.
 * <p>
 * Every trait and ability below is named ({@link IdentifiedTrait#named}/
 * {@link IdentifiedAbility#named}), never anonymous - so a later rank step, or a per-level clone
 * (see {@code EnemyCatalog.cloneAndAdjust}), can replace one by id instead of only ever being
 * able to add a second, competing entry alongside it.
 * <p>
 * {@code WARDEN_1}/{@code WARDEN_EGG_1}/{@code WARDEN_2}/{@code WARDEN_EGG_2}/{@code WARDEN_3}/
 * {@code WARDEN_EGG_3} are the boss encounter's finite, six-definition, strictly linear spawn
 * chain (see {@code docs/features/FEATURE-enemy-traits-and-effects.md}'s V1 Scope): each Warden's on-death
 * ability spawns its own stage's egg; each egg's {@code Once} ability hatches into the next
 * (weaker) Warden stage if left alive for its full delay, via {@code consumesSelf} - a
 * transformation, not a kill. The final egg carries no ability at all, so the encounter is
 * guaranteed to terminate. These six stay single-rank (registered as plain
 * {@link EnemyDefinition}s, resolving to {@link Rank#GRUNT} at every requested rank) - the Warden
 * fight is staged through its own six hand-authored definitions already, not through rank.
 */
final class BuiltInEnemies {

    // Bounty grows 2.5x per rank step (a fixed ratio, rounded to the nearest whole credit) while
    // health only doubles - so a tougher mob is worth disproportionately more, on top of already
    // costing more to kill. Health itself is untouched by this pass; only price below changed.
    static final RankedEnemy SIMPLE = RankedEnemy
            .startingAt(EnemyDefinition.of("c", "Simple mob", 50, 2, 1.28f, BodyArchetype.CIRCLE)
                    .withDescription("No special abilities."))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(100, 5))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(200, 13))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(400, 33)
                    .withDescription("No special abilities, but a faint layer of armor has started forming.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("armor", new PercentResistTrait(0.85f)))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(800, 83)
                    .withDescription("No special abilities, but its armor has grown formidable.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("armor", new PercentResistTrait(0.7f)))))
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
                    // Percentage listed before flat: DefinedEnemyMob.absorb folds every Trait.onHit
                    // in this list's own order, so "resist" runs first and "flatResist" subtracts
                    // its flat amount from whatever the percentage already let through, not the
                    // reverse.
                    .withAdditionalTraits(List.of(
                            IdentifiedTrait.named("resist", new PercentResistTrait(0.6f)),
                            IdentifiedTrait.named("flatResist", new FlatResistTrait(80)))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1280, 125)
                    .withDescription("Takes drastically less damage, and its plating blunts a large flat chunk of "
                            + "every hit outright. Immune to critical hits.")
                    .withAdditionalTraits(List.of(
                            IdentifiedTrait.named("resist", new PercentResistTrait(0.5f)),
                            IdentifiedTrait.named("flatResist", new FlatResistTrait(120)))))
            .build();

    // The Boss step's own ability constants - named rather than inlined since the FRENZIED
    // Boss's health-threshold spawn and neglect-heal each combine three or more numbers that
    // read better with a name than as bare literals inside the definition below.
    private static final float FRENZIED_SPAWN_HEALTH_THRESHOLD = 0.5f;
    // A brood of three spawnlings, spaced apart in time by AbilitySpawnShape.brood so they
    // trail one another rather than appearing stacked on the Boss's exact position.
    private static final int FRENZIED_SPAWN_COUNT = 3;
    private static final int FRENZIED_NEGLECT_WINDOW_TICKS = 200;
    private static final int FRENZIED_NEGLECT_HEAL_PER_TICK = 2;
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
                            IdentifiedTrait.named("criticalImmune", new CriticalImmunityTrait())))
                    .withMovement(new RotorMovement((float) Math.toRadians(-10.0)))
                    .withAdditionalAbilities(List.of(
                            IdentifiedAbility.named("spawn", new Ability(new HealthThresholdTrigger(FRENZIED_SPAWN_HEALTH_THRESHOLD),
                                    new SpawnEnemiesAction("tSpawn", AbilitySpawnShape.brood(FRENZIED_SPAWN_COUNT, 1f, 1f), false))),
                            IdentifiedAbility.named("neglectPenalty", new Ability(new TimeSinceLastHitTrigger(FRENZIED_NEGLECT_WINDOW_TICKS),
                                    new ApplyEffectAction(new HealTemplate(FRENZIED_NEGLECT_HEAL_PER_TICK, FRENZIED_NEGLECT_HEAL_DURATION_TICKS),
                                            new SelfTarget()))))))
            .build();
    // A single-rank reinforcement, not a RankedEnemy ladder - the Boss's brood ability always
    // spawns this one shape, the same precedent WARDEN_EGG_1/2/3 already set for ability-spawned
    // content that never needs to scale with the caster's own rank.
    private static final int T_SPAWN_DEATH_HEAL_PER_TICK = 4;
    private static final int T_SPAWN_DEATH_HEAL_DURATION_TICKS = 40;
    // "Big area" - matches the Warden's own callToArms radius, this codebase's existing "big"
    // scale (Mender's own heal radius is 80f).
    private static final float T_SPAWN_DEATH_HEAL_RADIUS = 150f;
    static final EnemyDefinition T_SPAWN = EnemyDefinition
            .of("tSpawn", "Frenzy Spawnling", 140, 2, 1.28f, BodyArchetype.TRIANGLE)
            .withDescription("A spawnling of an enraged Frenzied boss. Speeds up as it is hurt, and heals nearby "
                    + "allies with its dying breath.")
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.4f))))
            .withIdentifiedAbilities(List.of(IdentifiedAbility.named("deathHeal", new Ability(
                    new OnDeathTrigger(),
                    new ApplyEffectAction(new HealTemplate(T_SPAWN_DEATH_HEAL_PER_TICK, T_SPAWN_DEATH_HEAL_DURATION_TICKS),
                            new RadiusTarget(T_SPAWN_DEATH_HEAL_RADIUS))))));
    // 20 ticks/second (see TickRate) - 10 seconds, same conversion EGG_HATCH_DELAY_TICKS uses.
    private static final int GHOST_VANISH_DURATION_TICKS = 200;
    // Reapplied every second to every ally still in radius; each application's own duration
    // outlasts the interval so a lingering ally is never visible for even one tick between
    // refreshes - see ActiveEffects' "always extends to the longer remaining duration" rule.
    private static final int GHOST_SHROUD_INTERVAL_TICKS = 20;
    private static final int GHOST_SHROUD_DURATION_TICKS = 40;
    private static final float GHOST_SHROUD_RADIUS = 100f;
    static final RankedEnemy GHOST = RankedEnemy
            .startingAt(EnemyDefinition.of("g", "Ghost mob", 100, 4, 1.28f, BodyArchetype.GHOST)
                    .withDescription("An ordinary mob that turns invisible to towers for a while "
                            + "the first time it's hit. Area damage still finds it.")
                    .withHealthDivisor(5f)
                    .withIdentifiedAbilities(List.of(IdentifiedAbility.named("vanish", new Ability(
                            // A hit that also freezes this mob on the same tick suppresses this
                            // cast rather than delaying it: DefinedEnemyMob.isIncapacitated()
                            // gates every ability while FREEZE is active, including this
                            // fire-once trigger, so a frozen mob simply never gets to vanish for
                            // that hit instead of vanishing once the freeze wears off.
                            new OnFirstDamageTakenTrigger(),
                            new ApplyEffectAction(new InvisibleTemplate(GHOST_VANISH_DURATION_TICKS), new SelfTarget()))))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(200, 10))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(400, 25))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(800, 63)
                    .withDescription("Turns invisible to towers for a while the first time it's "
                            + "hit, and permanently shrouds every other ally near it - itself excluded.")
                    .withAdditionalAbilities(List.of(IdentifiedAbility.named("shroud", new Ability(
                            new PeriodicTrigger(GHOST_SHROUD_INTERVAL_TICKS),
                            new ApplyEffectAction(new InvisibleTemplate(GHOST_SHROUD_DURATION_TICKS), new RadiusTarget(GHOST_SHROUD_RADIUS)))))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1600, 158))
            .build();
    private static final int MENDER_HEAL_INTERVAL_TICKS = 20;
    private static final int MENDER_HEAL_DURATION_TICKS = 40;
    private static final int MENDER_HEAL_PER_TICK = 2;
    private static final float MENDER_HEAL_RADIUS = 80f;
    // A mob left unattacked long enough quietly mends itself on top of whatever it's already
    // healing nearby - added at Veteran, and stronger at Boss (see that rank's own ability).
    private static final int MENDER_SELF_HEAL_WINDOW_TICKS = 100;
    private static final int MENDER_SELF_HEAL_DURATION_TICKS = 100;
    static final RankedEnemy MENDER = RankedEnemy
            .startingAt(EnemyDefinition.of("m", "Mender mob", 60, 3, 1.28f, BodyArchetype.MENDER)
                    // "Other" is load-bearing, not flavor: RadiusTarget's own "every other
                    // valid-target enemy" semantics is what excludes the Mender itself from its
                    // own heal - see DefinedEnemyMob.MobAbilityContext.applyToOthersInRadius.
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
                            new ApplyEffectAction(new HealTemplate(1, MENDER_SELF_HEAL_DURATION_TICKS), new SelfTarget()))))))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(480, 50)
                    .withDescription("Its healing pulse now lasts twice as long, and a personal layer of armor "
                            + "joins its own quiet self-repair when left unattacked.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("armor", new PercentResistTrait(0.8f))))
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
                                    new ApplyEffectAction(new HealTemplate(2, MENDER_SELF_HEAL_DURATION_TICKS), new SelfTarget()))))))
            .build();
    static final EnemyDefinition WARDEN_EGG_3 = EnemyDefinition
            .of("wardenEgg3", "Warden's Final Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Must be defeated to end the encounter - it will not hatch again.");
    // 20 ticks/second at 1.0x tick speed (GameLoop.BASE_TICK_NANOS = 50ms) - 8 seconds.
    private static final int EGG_HATCH_DELAY_TICKS = 160;
    static final EnemyDefinition WARDEN_EGG_1 = EnemyDefinition
            .of("wardenEgg1", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time. Immune to critical hits, and "
                    + "its armor blunts part of every hit.")
            .withIdentifiedTraits(List.of(
                    IdentifiedTrait.named("criticalImmune", new CriticalImmunityTrait()),
                    IdentifiedTrait.named("armor", new PercentResistTrait(0.6f))))
            .withIdentifiedAbilities(List.of(IdentifiedAbility.named("hatch", new Ability(
                    new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden2", 1, true)))));
    static final EnemyDefinition WARDEN_EGG_2 = EnemyDefinition
            .of("wardenEgg2", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time. Immune to burn and freeze, "
                    + "and its plating blunts a flat chunk of every hit.")
            .withIdentifiedTraits(List.of(
                    IdentifiedTrait.named("burnImmune", new BurnImmunityTrait()),
                    IdentifiedTrait.named("freezeImmune", new FreezeImmunityTrait()),
                    IdentifiedTrait.named("armor", new FlatResistTrait(100))))
            .withIdentifiedAbilities(List.of(IdentifiedAbility.named("hatch", new Ability(
                    new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden3", 1, true)))));
    // Raised from an original 100 (itself raised from 15 - see git history) specifically so an
    // un-upgraded weak tower does zero damage against the Warden: 200 fully absorbs
    // CinderTower's 150 burn and PulseTower's 200 hit, matching the design intent that a player
    // must upgrade, or bring a heavier tower, to hurt it at all - while still meaningfully
    // denting a big single hit (SniperTower's 4000) the way the Warden's own doc comment
    // ("armor") implies it should. WARDEN_2/WARDEN_3 halve/quarter this as the boss weakens
    // across its own egg-hatch chain, so a tower that could do nothing against WARDEN_1 starts
    // landing real damage by the time it faces WARDEN_3.
    private static final int WARDEN_FLAT_RESIST = 200;
    // Shared by all three stages, appended to each stage's own flavor sentence - every stage
    // carries the exact same WARDEN_STANDING_ABILITIES plus its own on-death egg spawn, so one
    // description keeps the three in agreement instead of drifting the way WARDEN_2/WARDEN_3
    // used to (neither mentioned any ability at all).
    private static final String WARDEN_ABILITY_BLURB = " Periodically calls a reinforcement and re-shields itself; "
            + "shields every nearby ally once below half health; calls an extra reinforcement if left unattacked "
            + "too long; gains a shield whenever it survives a critical hit; and leaves behind an egg on death.";
    private static final List<IdentifiedAbility> WARDEN_STANDING_ABILITIES = List.of(
            // periodically calls for a reinforcement
            IdentifiedAbility.named("reinforce", new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false))),
            // periodically re-shields itself on top of its permanent armor trait
            IdentifiedAbility.named("reshield", new Ability(new PeriodicTrigger(400),
                    new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget()))),
            // at half health, shields every nearby ally - a one-time "call to arms"
            IdentifiedAbility.named("callToArms", new Ability(new HealthThresholdTrigger(0.5f),
                    new ApplyEffectAction(new ShieldTemplate(0.3f, 150), new RadiusTarget(150f)))),
            // punishes being ignored by healing itself - TimeSinceLastHitTrigger re-arms once
            // ticksSinceLastHit drops back below its window (i.e. the Warden is hit again) and
            // then reaches the window a second time, so this fires every time it's left alone for
            // 200 ticks, not just the first - see AbilityEvaluator.fireTimeSinceLastHit.
            IdentifiedAbility.named("neglectPenalty", new Ability(new TimeSinceLastHitTrigger(200),
                    new ApplyEffectAction(new HealTemplate(4, 40), new SelfTarget()))),
            // shields itself every time it survives a critical hit - repeatable, unlike the
            // fire-once triggers above
            IdentifiedAbility.named("critShield", new Ability(new OnCriticalHitTakenTrigger(),
                    new ApplyEffectAction(new ShieldTemplate(0.3f, 30), new SelfTarget()))));
    static final EnemyDefinition WARDEN_1 = EnemyDefinition
            .of("warden1", "The Warden", 8000, 100, 0.8f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new FlatResistTrait(WARDEN_FLAT_RESIST))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg1"));
    static final EnemyDefinition WARDEN_2 = EnemyDefinition
            .of("warden2", "The Weakened Warden", 5000, 100, 0.8f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, worn down from its last hatching." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new FlatResistTrait(WARDEN_FLAT_RESIST / 2))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg2"));
    static final EnemyDefinition WARDEN_3 = EnemyDefinition
            .of("warden3", "The Exhausted Warden", 3000, 100, 0.8f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, barely standing." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new FlatResistTrait(WARDEN_FLAT_RESIST / 4))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg3"));

    private BuiltInEnemies() {
    }

    private static List<IdentifiedAbility> wardenAbilities(String eggId) {
        List<IdentifiedAbility> abilities = new ArrayList<>(WARDEN_STANDING_ABILITIES);
        abilities.add(IdentifiedAbility.named("hatchEgg", new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(eggId, 1, false))));
        return List.copyOf(abilities);
    }
}
