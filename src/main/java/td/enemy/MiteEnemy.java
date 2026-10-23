package td.enemy;

import java.util.List;

/**
 * The Mite ({@code mite}): tiny, fragile and hard to aim at, meant to be sent in a {@code swarm}; it asks for area
 * damage. At Boss it bursts into a brood of its own, a definition of its own because a mite that spawns mites would
 * be a cycle.
 */
final class MiteEnemy {

    private MiteEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("mite", "Mite", 15, 1, 1.5f, BodyArchetype.MITE)
                        .withDescription("Tiny and quick, it comes in swarms and dies to anything.")
                        .withAgility(10f))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(27, 1).withAgility(15f))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(49, 2).withAgility(20f))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(88, 3)
                        .withStats(5f, 5f, 0f, 0f, 10f, 25f, 10f))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(158, 6)
                        .withStats(5f, 5f, 0f, 0f, 15f, 35f, 15f)
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(AbilityTerm.BROOD, new OnDeathTrigger(),
                                new SpawnEnemiesAction("miteBrood", AbilitySpawnShape.brood(3, 1f, 1f), false))))))
                .build());
        catalog.register(EnemyDefinition.of("miteBrood", "Mite Broodling", 23, 1, 1.7f, BodyArchetype.MITE)
                .withDescription("What a dying Mite boss leaves behind.")
                .withAgility(25f));
    }
}
