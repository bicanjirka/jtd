package td.enemy;

import td.effect.ShieldTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The built-in {@link EnemyDefinition}s. {@link EnemyCatalog#builtIn()} pre-registers all of
 * these under their wave-script ids. Two groups:
 * <p>
 * {@code CIRCLE}/{@code SQUARE}/{@code TRIANGLE}/{@code GHOST} are migrated from the old
 * {@code EnemyMobCircle}/{@code Square}/{@code Triangle}/{@code Ghost} leaf classes, reproducing
 * their exact prior wave-spawned behavior via {@link DefinedEnemyMob} - their own
 * {@code baseHealth}/{@code price} are placeholders only ever consulted if something
 * ability-spawns one directly (the Warden's reinforcement ability does, for {@code CIRCLE}).
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

    // 20 ticks/second at 1.0x tick speed (GameLoop.BASE_TICK_NANOS = 50ms) - 8 seconds.
    private static final int EGG_HATCH_DELAY_TICKS = 160;
    // Tuned, not a placeholder: raised from an original 15 (see git history), which was
    // negligible against every attack tower's actual per-hit/per-tick damage (150-4000, see
    // TowerOne.damage..TowerCinder.damage) - a reduction that small is a rounding error, not
    // armor. 100 stays below every tower's smallest per-application damage (TowerFour's 200,
    // TowerCinder's 150 burn) so no tower is fully negated by Damage's zero-clamp, while still
    // meaningfully denting a big single hit (TowerOne's 4000) the way the Warden's own doc
    // comment ("armor") implies it should.
    private static final int WARDEN_FLAT_RESIST = 100;

    private static final List<Ability> WARDEN_STANDING_ABILITIES = List.of(
            // periodically calls for a reinforcement
            new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false)),
            // periodically re-shields itself on top of its permanent armor trait
            new Ability(new PeriodicTrigger(400), new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget())),
            // at half health, shields every nearby ally - a one-time "call to arms"
            new Ability(new HealthThresholdTrigger(0.5f), new ApplyEffectAction(new ShieldTemplate(0.3f, 150), new RadiusTarget(150f))),
            // punishes being ignored with a bonus reinforcement
            new Ability(new TimeSinceLastHitTrigger(200), new SpawnEnemiesAction("c", 1, false)));

    private static List<Ability> wardenAbilities(String eggId) {
        List<Ability> abilities = new ArrayList<>(WARDEN_STANDING_ABILITIES);
        abilities.add(new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(eggId, 1, false)));
        return List.copyOf(abilities);
    }

    static final EnemyDefinition CIRCLE = new EnemyDefinition(
            "c", "Simple mob", "No special abilities.",
            50, 2, 1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.CIRCLE, new FixedMovement(),
            List.of(), List.of());

    static final EnemyDefinition SQUARE = new EnemyDefinition(
            "s", "Square mob", "Takes less damage.",
            80, 3, 1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.SQUARE, new RotorMovement((float) Math.toRadians(5.0)),
            List.of(new PercentResistTrait(0.8f, 0.05f)), List.of());

    static final EnemyDefinition TRIANGLE = new EnemyDefinition(
            "t", "Triangle mob", "Increases speed as it takes damage.",
            60, 3, 1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.TRIANGLE, new RotorMovement((float) Math.toRadians(-5.0)),
            List.of(new HurtSpeedTrait(1.4f, 0.1f)), List.of());

    static final EnemyDefinition GHOST = new EnemyDefinition(
            "g", "Ghost mob", "Invisible to all towers. Area damage hurts them.",
            100, 4, 1.28f, 5f, EnemyMob.type.Invisible,
            BodyArchetype.GHOST, new FixedMovement(),
            List.of(), List.of());

    static final EnemyDefinition WARDEN_1 = new EnemyDefinition(
            "warden1", "The Warden", "A hulking armored sentinel. Calls for reinforcements and shields itself and its allies.",
            8000, 100, 1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.SQUARE, new RotorMovement((float) Math.toRadians(2.0)),
            List.of(new FlatResistTrait(WARDEN_FLAT_RESIST)), wardenAbilities("wardenEgg1"));

    static final EnemyDefinition WARDEN_EGG_1 = new EnemyDefinition(
            "wardenEgg1", "Warden's Egg", "Hatches into a weaker Warden if not defeated in time.",
            1500, 20, 0f, 1f, EnemyMob.type.Normal,
            BodyArchetype.EGG, new FixedMovement(),
            List.of(), List.of(new Ability(new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden2", 1, true))));

    static final EnemyDefinition WARDEN_2 = new EnemyDefinition(
            "warden2", "The Weakened Warden", "A hulking armored sentinel, worn down from its last hatching.",
            5000, 100, 1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.SQUARE, new RotorMovement((float) Math.toRadians(2.0)),
            List.of(new FlatResistTrait(WARDEN_FLAT_RESIST)), wardenAbilities("wardenEgg2"));

    static final EnemyDefinition WARDEN_EGG_2 = new EnemyDefinition(
            "wardenEgg2", "Warden's Egg", "Hatches into a weaker Warden if not defeated in time.",
            1500, 20, 0f, 1f, EnemyMob.type.Normal,
            BodyArchetype.EGG, new FixedMovement(),
            List.of(), List.of(new Ability(new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden3", 1, true))));

    static final EnemyDefinition WARDEN_3 = new EnemyDefinition(
            "warden3", "The Exhausted Warden", "A hulking armored sentinel, barely standing.",
            3000, 100, 1.28f, 1f, EnemyMob.type.Normal,
            BodyArchetype.SQUARE, new RotorMovement((float) Math.toRadians(2.0)),
            List.of(new FlatResistTrait(WARDEN_FLAT_RESIST)), wardenAbilities("wardenEgg3"));

    static final EnemyDefinition WARDEN_EGG_3 = new EnemyDefinition(
            "wardenEgg3", "Warden's Final Egg", "Must be defeated to end the encounter - it will not hatch again.",
            1500, 20, 0f, 1f, EnemyMob.type.Normal,
            BodyArchetype.EGG, new FixedMovement(),
            List.of(), List.of());

    private BuiltInEnemies() {
    }
}
