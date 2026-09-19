package td.enemy;

import td.effect.ShieldTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The built-in {@link RankedEnemy} ladders and standalone {@link EnemyDefinition}s.
 * {@link EnemyCatalog#builtIn()} pre-registers all of these under their wave-script ids. Two
 * groups:
 * <p>
 * {@code SIMPLE}/{@code ARMORED}/{@code FRENZIED}/{@code GHOST} are the four basic mobs, each a
 * full five-rank ladder - every rank above {@link Rank#GRUNT} simply doubles the rank before it's
 * health and scales its bounty to match, {@code withHealthAndPrice} being the only thing each
 * step changes, except {@code SIMPLE} itself: its own ladder is this feature's demonstration that
 * a later rank can both add a trait (at {@link Rank#ELITE}) and replace it with a stronger one
 * (at {@link Rank#BOSS}) via the same identified-trait mechanism, following the shape of the
 * request's own pseudocode. <strong>A definition is named for what it does; its
 * {@link BodyArchetype} is what names the shape it is drawn as</strong> - so {@code ARMORED} is
 * a square and {@code FRENZIED} a triangle, the same way {@code SniperTower} is drawn as a
 * triangle. Every number here is still a placeholder for a later balance pass, the same as
 * before this feature - only the axis they scale along changed, from a wave-authored level to an
 * enemy-authored rank.
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

    static final RankedEnemy SIMPLE = RankedEnemy
            .startingAt(EnemyDefinition.of("c", "Simple mob", 50, 2, 1.28f, BodyArchetype.CIRCLE)
                    .withDescription("No special abilities."))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(100, 3))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(200, 5))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(400, 8)
                    .withDescription("No special abilities, but a faint shield has started forming.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("shield", new PercentResistTrait(0.85f)))))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(800, 15)
                    .withDescription("No special abilities, but its shield has grown formidable.")
                    .withAdditionalTraits(List.of(IdentifiedTrait.named("shield", new PercentResistTrait(0.7f)))))
            .build();
    static final RankedEnemy ARMORED = RankedEnemy
            .startingAt(EnemyDefinition.of("s", "Armored mob", 80, 3, 1.28f, BodyArchetype.SQUARE)
                    .withDescription("Takes less damage. Immune to critical hits.")
                    .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("resist", new PercentResistTrait(0.8f)),
                            IdentifiedTrait.named("criticalImmune", new CriticalImmunityTrait()))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(160, 5))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(320, 8))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(640, 13))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1280, 22))
            .build();
    static final RankedEnemy FRENZIED = RankedEnemy
            .startingAt(EnemyDefinition.of("t", "Frenzied mob", 60, 3, 1.28f, BodyArchetype.TRIANGLE)
                    .withDescription("Increases speed as it takes damage.")
                    .withMovement(new RotorMovement((float) Math.toRadians(-5.0)))
                    .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.4f)))))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(120, 5))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(240, 8))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(480, 13))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(960, 22))
            .build();
    static final RankedEnemy GHOST = RankedEnemy
            .startingAt(EnemyDefinition.of("g", "Ghost mob", 100, 4, 1.28f, BodyArchetype.GHOST)
                    .withDescription("Invisible to all towers. Area damage hurts them.")
                    .withMobType(EnemyMob.Type.INVISIBLE)
                    .withHealthDivisor(5f))
            .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(200, 6))
            .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(400, 10))
            .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(800, 16))
            .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1600, 28))
            .build();
    static final EnemyDefinition WARDEN_EGG_3 = EnemyDefinition
            .of("wardenEgg3", "Warden's Final Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Must be defeated to end the encounter - it will not hatch again.");
    // 20 ticks/second at 1.0x tick speed (GameLoop.BASE_TICK_NANOS = 50ms) - 8 seconds.
    private static final int EGG_HATCH_DELAY_TICKS = 160;
    static final EnemyDefinition WARDEN_EGG_1 = EnemyDefinition
            .of("wardenEgg1", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time.")
            .withIdentifiedAbilities(List.of(IdentifiedAbility.named("hatch", new Ability(
                    new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden2", 1, true)))));
    static final EnemyDefinition WARDEN_EGG_2 = EnemyDefinition
            .of("wardenEgg2", "Warden's Egg", 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
            .withDescription("Hatches into a weaker Warden if not defeated in time.")
            .withIdentifiedAbilities(List.of(IdentifiedAbility.named("hatch", new Ability(
                    new OnceTrigger(EGG_HATCH_DELAY_TICKS), new SpawnEnemiesAction("warden3", 1, true)))));
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
    private static final List<IdentifiedAbility> WARDEN_STANDING_ABILITIES = List.of(
            // periodically calls for a reinforcement
            IdentifiedAbility.named("reinforce", new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false))),
            // periodically re-shields itself on top of its permanent armor trait
            IdentifiedAbility.named("reshield", new Ability(new PeriodicTrigger(400),
                    new ApplyEffectAction(new ShieldTemplate(0.5f, 100), new SelfTarget()))),
            // at half health, shields every nearby ally - a one-time "call to arms"
            IdentifiedAbility.named("callToArms", new Ability(new HealthThresholdTrigger(0.5f),
                    new ApplyEffectAction(new ShieldTemplate(0.3f, 150), new RadiusTarget(150f)))),
            // punishes being ignored with a bonus reinforcement
            IdentifiedAbility.named("neglectPenalty", new Ability(new TimeSinceLastHitTrigger(200),
                    new SpawnEnemiesAction("c", 1, false))),
            // shields itself every time it survives a critical hit - repeatable, unlike the
            // fire-once triggers above
            IdentifiedAbility.named("critShield", new Ability(new OnCriticalHitTakenTrigger(),
                    new ApplyEffectAction(new ShieldTemplate(0.3f, 100), new SelfTarget()))));
    static final EnemyDefinition WARDEN_1 = EnemyDefinition
            .of("warden1", "The Warden", 8000, 100, 1.28f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new FlatResistTrait(WARDEN_FLAT_RESIST))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg1"));
    static final EnemyDefinition WARDEN_2 = EnemyDefinition
            .of("warden2", "The Weakened Warden", 5000, 100, 1.28f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, worn down from its last hatching." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new FlatResistTrait(WARDEN_FLAT_RESIST))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg2"));
    static final EnemyDefinition WARDEN_3 = EnemyDefinition
            .of("warden3", "The Exhausted Warden", 3000, 100, 1.28f, BodyArchetype.WARDEN)
            .withDescription("A hulking armored sentinel, barely standing." + WARDEN_ABILITY_BLURB)
            .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
            .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", new FlatResistTrait(WARDEN_FLAT_RESIST))))
            .withIdentifiedAbilities(wardenAbilities("wardenEgg3"));

    private BuiltInEnemies() {
    }

    private static List<IdentifiedAbility> wardenAbilities(String eggId) {
        List<IdentifiedAbility> abilities = new ArrayList<>(WARDEN_STANDING_ABILITIES);
        abilities.add(IdentifiedAbility.named("hatchEgg", new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(eggId, 1, false))));
        return List.copyOf(abilities);
    }
}
