package td.enemy;

import td.stat.EnemyStat;

import java.util.List;

/**
 * The Simple mob ({@code c}): no abilities. From Elite an armor that hardens against what hurts it most, and at Boss
 * a steadiness that blunts crits.
 */
final class SimpleEnemy {

    private SimpleEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("c", "Simple mob", 100, 3, 1.28f, BodyArchetype.CIRCLE)
                        .withDescription("No special abilities."), RankGrowth.of(2f, 3))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("No special abilities, but a layer of armor has formed, "
                                + "hardened against whichever damage type has hit hardest this level.")
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(67f)))))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("No special abilities, but twice an elite's bulk under the same armor, hardened "
                                + "against whichever damage type has hit hardest this level, and steady enough to "
                                + "blunt crits.")
                        .withStat(EnemyStat.RESILIENCE, 30f))
                .build());
    }
}
