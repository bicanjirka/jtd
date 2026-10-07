package td.enemy;

/** The Courier ({@code courier}): very fast and fragile, and pays well for the tower that catches it. */
final class CourierEnemy {

    private CourierEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("courier", "Courier", 30, 12, 3.0f, BodyArchetype.COURIER)
                        .withDescription("Very fast and fragile, with a high bounty. Worth hunting."))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(60, 30))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(120, 75))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(240, 188))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(480, 470))
                .build());
    }
}
