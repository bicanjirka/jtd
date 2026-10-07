package td.enemy;

import td.effect.HealTemplate;

import java.util.List;

/**
 * The Frenzied mob ({@code t}): faster the more it is hurt. At Boss rank it calls a brood of
 * spawnlings once badly hurt and heals itself when left alone; the spawnlings are a definition of
 * their own.
 */
final class FrenziedEnemy {

    private FrenziedEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
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
                                IdentifiedAbility.anonymous(new Ability(new HealthThresholdTrigger(0.5f),
                                        new SpawnEnemiesAction("tSpawn", AbilitySpawnShape.brood(3, 1f, 1f), false))),
                                IdentifiedAbility.anonymous(new Ability(new TimeSinceLastHitTrigger(200),
                                        new ApplyEffectAction(new HealTemplate(0.02f, 200), new SelfTarget()))))))
                .build());
        catalog.register(EnemyDefinition.of("tSpawn", "Frenzy Spawnling", 140, 2, 1.28f, BodyArchetype.TRIANGLE)
                .withDescription("A spawnling of an enraged Frenzied boss. Speeds up as it is hurt, and heals nearby "
                        + "allies with its dying breath.")
                .withTraits(List.of(new HurtSpeedTrait(1.4f)))
                .withAbilities(List.of(new Ability(new OnDeathTrigger(),
                        new ApplyEffectAction(new HealTemplate(0.04f, 40), new RadiusTarget(150f))))));
    }
}
