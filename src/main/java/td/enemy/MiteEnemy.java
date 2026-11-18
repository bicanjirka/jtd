package td.enemy;

import java.util.List;

/**
 * The Mite ({@code mite}): tiny, fragile and hard to aim at, meant to be sent in a {@code swarm}; it asks for area
 * damage. From Veteran it bursts into a brood when it dies, larger and tougher with rank, and each Broodling bursts
 * once more into Hatchlings; both are definitions of their own because a mite that spawns mites would be a cycle.
 */
final class MiteEnemy {

    private MiteEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("mite", "Mite", 20, 1, 1.5f, BodyArchetype.MITE)
                        .withDescription("Tiny and quick, it comes in swarms and dies to anything.")
                        .withAgility(30f), RankGrowth.of(14f))
                .thenAt(Rank.SOLDIER, e -> e.withAgility(35f))
                .thenAt(Rank.VETERAN, e -> e
                        .withAgility(40f)
                        .withTenacity(15f)
                        .withAdditionalAbilities(List.of(brood(2, 1f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(20f, 20f, 0f, 0f, 65f, 15f, 15f, 10f)
                        .withAdditionalAbilities(List.of(brood(4, 3f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(35f, 35f, 0f, 0f, 90f, 25f, 25f, 25f)
                        .withAdditionalAbilities(List.of(brood(9, 5f))))
                .build());
        catalog.register(EnemyDefinition.of("miteBroodling", "Mite Broodling", 75, 0, 1.7f, BodyArchetype.MITE)
                .withDescription("What a dying Mite leaves behind. It bursts once more when it dies.")
                .withAgility(25f)
                .withAbilities(List.of(new Ability(AbilityTerm.BROOD, new OnDeathTrigger(),
                        new SpawnEnemiesAction("miteHatchling", AbilitySpawnShape.brood(3, 1f, 0f), false)))));
        catalog.register(EnemyDefinition.of("miteHatchling", "Mite Hatchling", 30, 0, 1.7f, BodyArchetype.MITE)
                .withDescription("The last of a Mite's brood.")
                .withAgility(25f));
    }

    /** {@code broodlings} unpaid Broodlings, each {@code healthMultiplier} times a Broodling's health. */
    private static IdentifiedAbility brood(int broodlings, float healthMultiplier) {
        return IdentifiedAbility.named("brood", new Ability(AbilityTerm.BROOD, new OnDeathTrigger(),
                new SpawnEnemiesAction("miteBroodling", AbilitySpawnShape.brood(broodlings, healthMultiplier, 0f), false)));
    }
}
