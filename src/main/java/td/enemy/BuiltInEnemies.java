package td.enemy;

import td.effect.ShieldTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The built-in {@link EnemyDefinition}s. {@link EnemyCatalog#builtIn()} pre-registers all of
 * these under their wave-script ids. Two groups:
 * <p>
 * {@code SIMPLE}/{@code ARMORED}/{@code FRENZIED}/{@code GHOST} are the four basic mobs,
 * reproducing via {@link DefinedEnemyMob} the exact wave-spawned behavior of the per-type leaf
 * classes that used to exist. <strong>A definition is named for what it does; its
 * {@link BodyArchetype} is what names the shape it is drawn as</strong> - so {@code ARMORED} is
 * a square and {@code FRENZIED} a triangle, the same way {@code SniperTower} is drawn as a
 * triangle. Their own
 * {@code baseHealth}/{@code price} are placeholders only ever consulted if something
 * ability-spawns one directly (the Warden's reinforcement ability does, for {@code SIMPLE}).
 * <p>
 * {@code WARDEN_1}/{@code WARDEN_EGG_1}/{@code WARDEN_2}/{@code WARDEN_EGG_2}/{@code WARDEN_3}/
 * {@code WARDEN_EGG_3} are the boss encounter's finite, six-definition, strictly linear spawn
 * chain (see {@code docs/features/FEATURE-enemy-traits-and-effects.md}'s V1 Scope): each Warden's on-death
 * ability spawns its own stage's egg; each egg's {@code Once} ability hatches into the next
 * (weaker) Warden stage if left alive for its full delay, via {@code consumesSelf} - a
 * transformation, not a kill. The final egg carries no ability at all, so the encounter is
 * guaranteed to terminate. Most numbers here (health, price, ability intervals, shield
 * percentages) are still placeholders for a later balance pass, like every other number in
 * this feature - {@link #WARDEN_FLAT_RESIST} is the one exception, tuned against the actual
 * per-hit damage scale every attack tower operates at (see its own comment).
 */
final class BuiltInEnemies {

    static final EnemyDefinition SIMPLE = EnemyDefinition
            .of("c", "Simple mob", 50, 2, 1.28f, BodyArchetype.CIRCLE)
            .withDescription("No special abilities.");
    static final EnemyDefinition ARMORED = EnemyDefinition
            .of("s", "Armored mob", 80, 3, 1.28f, BodyArchetype.SQUARE)
            .withDescription("Takes less damage. Immune to critical hits.")
            .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
            .withTraits(List.of(new PercentResistTrait(0.8f), new CriticalImmunityTrait()));
    static final EnemyDefinition FRENZIED = EnemyDefinition
            .of("t", "Frenzied mob", 60, 3, 1.28f, BodyArchetype.TRIANGLE)
            .withDescription("Increases speed as it takes damage.")
            .withMovement(new RotorMovement((float) Math.toRadians(-5.0)))
            .withTraits(List.of(new HurtSpeedTrait(1.4f)));
    static final EnemyDefinition GHOST = EnemyDefinition
            .of("g", "Ghost mob", 100, 4, 1.28f, BodyArchetype.GHOST)
            .withDescription("Invisible to all towers. Area damage hurts them.")
            .withMobType(EnemyMob.Type.INVISIBLE)
            .withHealthDivisor(5f);
    static final EnemyDefinition WARDEN_EGG_3 = EnemyDefinition
            .of("wardenEgg3", "Warden's Final Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Must be defeated to end the encounter - it will not hatch again.");
    // 20 ticks/second at 1.0x tick speed (GameLoop.BASE_TICK_NANOS = 50ms) - 8 seconds.
    private static final int EGG_HATCH_DELAY_TICKS = 160;
    static final EnemyDefinition WARDEN_EGG_1 = EnemyDefinition
            .of("wardenEgg1", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time.")
            .withAbilities(List.of(new Ability(new OnceTrigger(EGG_HATCH_DELAY_TICKS),
                    new SpawnEnemiesAction("warden2", 1, true))));
    static final EnemyDefinition WARDEN_EGG_2 = EnemyDefinition
            .of("wardenEgg2", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time.")
            .withAbilities(List.of(new Ability(new OnceTrigger(EGG_HATCH_DELAY_TICKS),
                    new SpawnEnemiesAction("warden3", 1, true))));
    // Tuned, not a placeholder: raised from an original 15 (see git history), which was
    // negligible against every attack tower's actual per-hit/per-tick damage (150-4000, see
    // SniperTower.DAMAGE..CinderTower.DAMAGE) - a reduction that small is a rounding error, not
    // armor. 100 stays below every tower's smallest per-application damage (PulseTower's 200,
    // CinderTower's 150 burn) so no tower is fully negated by Damage's zero-clamp, while still
    // meaningfully denting a big single hit (SniperTower's 4000) the way the Warden's own doc
    // comment ("armor") implies it should.
    private static final int WARDEN_FLAT_RESIST = 100;
    // Shared by all three stages, appended to each stage's own flavor sentence - every stage
    // carries the exact same WARDEN_STANDING_ABILITIES plus its own on-death egg spawn, so one
    // description keeps the three in agreement instead of drifting the way WARDEN_2/WARDEN_3
    // used to (neither mentioned any ability at all).
    private static final String WARDEN_ABILITY_BLURB = " Periodically calls a reinforcement and re-shields itself; "
            + "shields every nearby ally once below half health; calls an extra reinforcement if left unattacked "
            + "too long; gains a shield whenever it survives a critical hit; and leaves behind an egg on death.";
    private static final List<Ability> WARDEN_STANDING_ABILITIES = List.of(
            // periodically calls for a reinforcement
            new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false)),
            // periodically re-shields itself on top of its permanent armor trait
            new Ability(new PeriodicTrigger(400), new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget())),
            // at half health, shields every nearby ally - a one-time "call to arms"
            new Ability(new HealthThresholdTrigger(0.5f), new ApplyEffectAction(new ShieldTemplate(0.3f, 150), new RadiusTarget(150f))),
            // punishes being ignored with a bonus reinforcement
            new Ability(new TimeSinceLastHitTrigger(200), new SpawnEnemiesAction("c", 1, false)),
            // shields itself every time it survives a critical hit - repeatable, unlike the
            // fire-once triggers above
            new Ability(new OnCriticalHitTakenTrigger(), new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new SelfTarget())));
    static final EnemyDefinition WARDEN_1 = EnemyDefinition
            .of("warden1", "The Warden", 8000, 100, 1.28f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withTraits(List.of(new FlatResistTrait(WARDEN_FLAT_RESIST)))
            .withAbilities(wardenAbilities("wardenEgg1"));
    static final EnemyDefinition WARDEN_2 = EnemyDefinition
            .of("warden2", "The Weakened Warden", 5000, 100, 1.28f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, worn down from its last hatching." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withTraits(List.of(new FlatResistTrait(WARDEN_FLAT_RESIST)))
            .withAbilities(wardenAbilities("wardenEgg2"));
    static final EnemyDefinition WARDEN_3 = EnemyDefinition
            .of("warden3", "The Exhausted Warden", 3000, 100, 1.28f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, barely standing." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withTraits(List.of(new FlatResistTrait(WARDEN_FLAT_RESIST)))
            .withAbilities(wardenAbilities("wardenEgg3"));

    private BuiltInEnemies() {
    }

    private static List<Ability> wardenAbilities(String eggId) {
        List<Ability> abilities = new ArrayList<>(WARDEN_STANDING_ABILITIES);
        abilities.add(new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(eggId, 1, false)));
        return List.copyOf(abilities);
    }
}
