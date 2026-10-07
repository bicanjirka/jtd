package td.enemy;

import java.util.List;

/** The Simple mob ({@code c}): no abilities, and from Elite an armor that hardens against what hurts it most. */
final class SimpleEnemy {

    private SimpleEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        // Bounty grows 2.5x per rank while health only doubles, so tougher mobs pay
        // disproportionately more.
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("c", "Simple mob", 50, 2, 1.28f, BodyArchetype.CIRCLE)
                        .withDescription("No special abilities."))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(100, 5))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(200, 13))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(400, 33)
                        .withDescription("No special abilities, but a layer of armor has formed, "
                                + "hardened against whichever damage type has hit hardest this level.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(0.6f)))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(800, 83)
                        .withDescription("No special abilities, but twice an elite's bulk under the same armor, hardened "
                                + "against whichever damage type has hit hardest this level."))
                .build());
    }
}
