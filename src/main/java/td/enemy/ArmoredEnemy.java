package td.enemy;

import td.util.TickRate;

import java.util.List;

/**
 * The Armored mob ({@code s}): less damage from every hit, immune to crits, and at Boss rank it calls
 * reinforcements. Those are a definition of their own, not the ladder's boss, because a boss that
 * reinforces with itself would be a spawn cycle.
 */
final class ArmoredEnemy {

    private ArmoredEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
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
                                + "every hit outright. Immune to critical hits. Calls three armored reinforcements "
                                + "every three seconds.")
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("flatResist", FlatResistTrait.physicalOnly(10)),
                                IdentifiedTrait.named("resist", new PercentResistTrait(0.5f))))
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(
                                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                                new SpawnEnemiesAction("sSpawn", AbilitySpawnShape.brood(3, 1f, 1f), false))))))
                .build());
        catalog.register(EnemyDefinition.of("sSpawn", "Armored Reinforcement", 100, 1, 1.28f, BodyArchetype.SQUARE)
                .withDescription("A reinforcement called by an Armored boss. Takes less damage.")
                .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                .withTraits(List.of(new PercentResistTrait(0.8f))));
    }
}
