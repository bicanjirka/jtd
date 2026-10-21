package td.enemy;

import td.effect.HealTemplate;
import td.effect.SprintTemplate;

import java.util.List;

/**
 * The Frenzied mob ({@code t}): faster the more it is hurt, and reckless, so crits land harder on it. From Elite it
 * bolts once badly hurt; at Boss rank it calls a brood of spawnlings, heals itself when left alone and no longer
 * takes crits; the spawnlings are a definition of their own.
 */
final class FrenziedEnemy {

    private FrenziedEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("t", "Frenzied mob", 120, 4, 1.28f, BodyArchetype.TRIANGLE)
                        .withDescription("Reckless: the more it is hurt, the faster it runs.")
                        .withMovement(new RotorMovement((float) Math.toRadians(-5.0)))
                        .withResilience(-20f)
                        .withIdentifiedTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.4f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(228, 7))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(433, 10)
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.5f)))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(823, 19)
                        .withStats(10f, 10f, 0f, 0f, -20f, 10f, 10f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(1.8f))))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.SECOND_WIND,
                                new HealthThresholdTrigger(0.5f),
                                new ApplyEffectAction(new SprintTemplate(40), new SelfTarget()))))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1564, 34)
                        .withStats(15f, 15f, 0f, 0f, 100f, 10f, 20f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(2f))))
                        .withMovement(new RotorMovement((float) Math.toRadians(-10.0)))
                        .withAdditionalAbilities(List.of(
                                IdentifiedAbility.anonymous(new Ability(AbilityTerm.SWARM, new HealthThresholdTrigger(0.5f),
                                        new SpawnEnemiesAction("tSpawn", AbilitySpawnShape.brood(3, 1f, 1f), false))),
                                IdentifiedAbility.anonymous(new Ability(AbilityTerm.RECOVER, new TimeSinceLastHitTrigger(200),
                                        new ApplyEffectAction(new HealTemplate(2f, 200), new SelfTarget()))))))
                .build());
        catalog.register(EnemyDefinition.of("tSpawn", "Frenzy Spawnling", 280, 2, 1.28f, BodyArchetype.TRIANGLE)
                .withDescription("Born of an enraged Frenzied boss; it tends its kin as it dies.")
                .withResilience(-20f)
                .withTraits(List.of(new HurtSpeedTrait(1.4f)))
                .withAbilities(List.of(new Ability(AbilityTerm.LAST_RITES, new OnDeathTrigger(),
                        new ApplyEffectAction(new HealTemplate(1f, 40), new RadiusTarget(150f))))));
    }
}
