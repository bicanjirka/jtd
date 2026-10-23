package td.enemy;

import java.util.List;

/**
 * The Simple mob ({@code c}): no abilities and, up to Veteran, nothing but health. The one ladder that derives its
 * health and bounty from a {@link RankGrowth}, so the reference mob shows the pattern the others write out. From Elite an armor that hardens
 * against what hurts it most and a little of every stat, and at Boss a steadiness that blunts crits.
 */
final class SimpleEnemy {

    private SimpleEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("c", "Simple mob", 75, 2, 1.28f, BodyArchetype.CIRCLE)
                        .withDescription("The plain foot soldier of every wave."), RankGrowth.of(2f, 2))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 0f, 0f, 10f, 5f, 10f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(67f)))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 15f, 0f, 0f, 30f, 5f, 20f))
                .build());
    }
}
