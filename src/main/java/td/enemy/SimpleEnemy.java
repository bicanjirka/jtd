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
                .startingAt(EnemyDefinition.of("c", "Simple mob", 100, 3, 1.28f, BodyArchetype.CIRCLE)
                        .withDescription("No special abilities."), RankGrowth.of(2f, 3))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("No special abilities, but a layer of armor has formed, "
                                + "hardened against whichever damage type has hit hardest this level.")
                        .withArmor(10f)
                        .withMagicResist(10f)
                        .withResilience(10f)
                        .withAgility(5f)
                        .withSpirit(10f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(67f)))))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("No special abilities, but twice an elite's bulk under the same armor, hardened "
                                + "against whichever damage type has hit hardest this level, and steady enough to "
                                + "blunt crits.")
                        .withArmor(15f)
                        .withMagicResist(15f)
                        .withResilience(30f)
                        .withSpirit(20f))
                .build());
    }
}
