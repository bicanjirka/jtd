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
                List.of(IdentifiedTrait.named("armor", new PercentResistTrait(0.8f)),
                        IdentifiedTrait.named("plating", new FlatResistTrait(1f)))));
        // High enough that an un-upgraded weak tower does nothing; later stages weaken it.
        catalog.register(warden("warden3", "The Exhausted Warden", 3000, "hulking armored sentinel, barely standing", 2.5f, "wardenEgg3"));
        catalog.register(egg("wardenEgg2", "Warden's Egg",
                "Hatches into a weaker Warden if not defeated in time. Immune to burn and freeze, "
                        + "and its armor and plating blunt every hit, though less than the first egg's.",
                List.of(IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                        IdentifiedTrait.named("armor", new PercentResistTrait(0.5f)),
                        IdentifiedTrait.named("plating", new FlatResistTrait(4f))))
                .withAbilities(hatchInto("warden3")));
        catalog.register(warden("warden2", "The Weakened Warden", 5000, "hulking armored sentinel, worn down from its last hatching", 5f,
                "wardenEgg2"));
        catalog.register(egg("wardenEgg1", "Warden's Egg",
                "Hatches into a weaker Warden if not defeated in time. Almost immune: it ignores critical "
                        + "hits, burn, freeze and chill, its armor stops most of every hit, and its plating blunts what "
                        + "is left.",
                List.of(IdentifiedTrait.anonymous(new CriticalImmunityTrait()),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                        IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.CHILL)),
                        IdentifiedTrait.named("armor", new PercentResistTrait(0.2f)),
                        IdentifiedTrait.named("plating", new FlatResistTrait(12f))))
                .withAbilities(hatchInto("warden2")));
        catalog.register(warden("warden1", "The Warden", 8000, "hulking armored sentinel", 10f, "wardenEgg1"));
    }

    private static EnemyDefinition egg(String id, String name, String description, List<IdentifiedTrait> traits) {
        return EnemyDefinition.of(id, name, 1500, 20, 0f, BodyArchetype.WARDEN_EGG)
                .withDescription(description)
                .withIdentifiedTraits(traits);
    }

    private static List<Ability> hatchInto(String wardenId) {
        return List.of(new Ability(new OnceTrigger(160), new SpawnEnemiesAction(wardenId, 1, true)));
    }

    /** One stage: "A {@code lead}.", the standing abilities every stage shares, and {@code eggId} left on death. */
    private static EnemyDefinition warden(String id, String name, int health, String lead, float plating,
                                          String eggId) {
        List<IdentifiedAbility> abilities = new ArrayList<>(List.of(
                IdentifiedAbility.anonymous(new Ability(new PeriodicTrigger(300), new SpawnEnemiesAction("c", 1, false))),
                // Its plating only answers physical hits, so its own shield covers the other type.
                IdentifiedAbility.anonymous(new Ability(new PeriodicTrigger(400),
                        new ApplyEffectAction(ShieldTemplate.magicOnly(0.5f, 100), new SelfTarget()))),
                IdentifiedAbility.anonymous(new Ability(new HealthThresholdTrigger(0.5f),
                        new ApplyEffectAction(new ShieldTemplate(0.3f, 150), new RadiusTarget(150f)))),
                // Re-arms after each hit, so it fires every time it is left alone, not just once.
                IdentifiedAbility.anonymous(new Ability(new TimeSinceLastHitTrigger(200),
                        new ApplyEffectAction(new HealTemplate(0.04f, 40), new SelfTarget()))),
                IdentifiedAbility.anonymous(new Ability(new OnCriticalHitTakenTrigger(),
                        new ApplyEffectAction(new ShieldTemplate(0.3f, 30), new SelfTarget())))));
        abilities.add(IdentifiedAbility.anonymous(new Ability(new OnDeathTrigger(),
                new SpawnEnemiesAction(eggId, 1, false))));
        return EnemyDefinition.of(id, name, health, 100, 0.8f, BodyArchetype.WARDEN)
                .withDescription("A " + lead + ". Periodically calls a reinforcement and shields itself against magic; "
                        + "shields every nearby ally once below half health; heals itself if left unattacked "
                        + "too long; gains a shield whenever it survives a critical hit; and leaves behind an egg on death.")
                .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                .withIdentifiedTraits(List.of(IdentifiedTrait.named("armor", FlatResistTrait.physicalOnly(plating))))
                .withIdentifiedAbilities(List.copyOf(abilities));
    }
}
