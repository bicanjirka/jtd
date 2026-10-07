package td.enemy;

/** The Mite ({@code mite}): tiny and fragile, meant to be sent in a {@code swarm}; it asks for area damage. */
final class MiteEnemy {

    private MiteEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("mite", "Mite", 10, 1, 1.5f, BodyArchetype.MITE)
                        .withDescription("Tiny and quick, and dies to a single hit. Sent in swarms."))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(20, 2))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(40, 5))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(80, 13))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(160, 33))
                .build());
    }
}
