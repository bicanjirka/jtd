package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.effect.ShieldTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The Warden boss chain: three Wardens, each weaker than the last, and the egg each leaves on death.
 * An egg hatches into the next Warden unless it is killed in time, and the last egg has no ability,
 * so the chain is finite and always ends.
 */
final class WardenEnemy {

    private WardenEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        // Protection, not health, is what makes a hatch likely: the first egg shrugs off all but the
        // heaviest hits and each later one gives something up, until the last can simply be shot.
        catalog.register(egg("wardenEgg3", "Warden's Final Egg",
                "Must be defeated to end the encounter - it will not hatch again. Its shell is cracked: "
                        + "only light armor and plating are left.",
                new Shell(25f, 1f, 0f), List.of()));
        // High enough that an un-upgraded weak tower does nothing; later stages weaken it.
        catalog.register(warden("warden3", "The Exhausted Warden", 6800, "hulking armored sentinel, barely standing",
                new Shell(5f, 2.5f, 0f), "wardenEgg3"));
        catalog.register(egg("wardenEgg2", "Warden's Egg",
                "Hatches into a weaker Warden if not defeated in time. Immune to burn and freeze, "
                        + "and its armor and plating blunt every hit, though less than the first egg's.",
                new Shell(100f, 4f, 0f),
                List.of(IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE))))
                .withAbilities(hatchInto("warden3")));
        catalog.register(warden("warden2", "The Weakened Warden", 11000,
                "hulking armored sentinel, worn down from its last hatching", new Shell(10f, 5f, 0f), "wardenEgg2"));
        catalog.register(egg("wardenEgg1", "Warden's Egg",
                "Hatches into a weaker Warden if not defeated in time. Almost immune: it ignores critical "
                        + "hits, burn, freeze and chill, its armor stops most of every hit, and its plating blunts what "
                        + "is left.",
                new Shell(400f, 12f, 100f),
                List.of(IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.CHILL))))
                .withAbilities(hatchInto("warden2")));
        catalog.register(warden("warden1", "The Warden", 18000, "hulking armored sentinel", new Shell(15f, 10f, 0f),
                "wardenEgg1"));
    }

    /**
     * What one stage of the chain is protected by.
     *
     * @param resist     armor and magic resist, in points
     * @param plating    plating, and insulation too on an egg; a Warden has no insulation
     * @param resilience against crits; 100 is crit-immune. A Warden has none: it answers a crit with a shield,
     *                   and resilience would only mute its own ability
     */
    private record Shell(float resist, float plating, float resilience) {
    }

    private static EnemyDefinition egg(String id, String name, String description, Shell shell,
                                       List<IdentifiedTrait> traits) {
        return EnemyDefinition.of(id, name, 3000, 20, 0f, BodyArchetype.WARDEN_EGG)
                .withDescription(description)
                .withStats(shell.resist(), shell.resist(), shell.plating(), shell.plating(), shell.resilience(), 0f, 0f)
                .withIdentifiedTraits(traits);
    }

    private static List<Ability> hatchInto(String wardenId) {
        return List.of(new Ability(new OnceTrigger(160), new SpawnEnemiesAction(wardenId, 1, true)));
    }

    /** One stage: "A {@code lead}.", the standing abilities every stage shares, and {@code eggId} left on death. */
    private static EnemyDefinition warden(String id, String name, int health, String lead, Shell shell,
                                          String eggId) {
        List<IdentifiedAbility> abilities = new ArrayList<>(List.of(
                IdentifiedAbility.anonymous(new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false))),
                // Its plating only answers physical hits, so its own shield covers the other type.
                IdentifiedAbility.anonymous(new Ability(new PeriodicTrigger(400),
                        new ApplyEffectAction(ShieldTemplate.magicOnly(health * 0.1f, 100), new SelfTarget()))),
                IdentifiedAbility.anonymous(new Ability(new HealthThresholdTrigger(0.5f),
                        new ApplyEffectAction(new ShieldTemplate(60f, 150), new RadiusTarget(150f)))),
                // Re-arms after each hit, so it fires every time it is left alone, not just once.
                IdentifiedAbility.anonymous(new Ability(new TimeSinceLastHitTrigger(200),
                        new ApplyEffectAction(new HealTemplate(health * 0.0015f, 100), new SelfTarget()))),
                IdentifiedAbility.anonymous(new Ability(new OnCriticalHitTakenTrigger(),
                        new ApplyEffectAction(new ShieldTemplate(health * 0.03f, 30), new SelfTarget())))));
        abilities.add(IdentifiedAbility.anonymous(new Ability(new OnDeathTrigger(),
                new SpawnEnemiesAction(eggId, 1, false))));
        return EnemyDefinition.of(id, name, health, 100, 0.8f, BodyArchetype.WARDEN)
                .withDescription("A " + lead + ". Periodically calls a reinforcement and shields itself against magic; "
                        + "shields every nearby ally once below half health; heals itself if left unattacked "
                        + "too long; gains a shield whenever it survives a critical hit; and leaves behind an egg on death. "
                        + "If it escapes, the level is lost.")
                .withLivesOnLeak(EnemyDefinition.ALL_LIVES)
                .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                .withStats(shell.resist(), shell.resist(), shell.plating(), 0f, shell.resilience(), 0f, 0f)
                .withIdentifiedAbilities(List.copyOf(abilities));
    }
}
