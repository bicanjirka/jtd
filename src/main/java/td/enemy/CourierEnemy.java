package td.enemy;

import java.util.List;

/** The Courier ({@code courier}): very fast, fragile and a little nimble, and pays well for the tower that catches it. */
final class CourierEnemy {

    private CourierEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("courier", "Courier", 30, 12, 3.0f, BodyArchetype.COURIER)
                        .withDescription("Very fast and fragile, nimble enough to dodge an aimed shot now and then, "
                                + "with a high bounty. Worth hunting.")
                        .withTraits(List.of(new AgilityTrait(25f))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(60, 30))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(120, 75))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(240, 188))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(480, 470))
                .build());
    }
}
