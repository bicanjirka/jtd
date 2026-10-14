package td.enemy;

import td.stat.EnemyStat;

import java.util.List;

/**
 * The Mite ({@code mite}): tiny and fragile, meant to be sent in a {@code swarm}; it asks for area damage. At Boss
 * it bursts into a brood of its own, a definition of its own because a mite that spawns mites would be a cycle.
 */
final class MiteEnemy {

    private MiteEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("mite", "Mite", 20, 1, 1.5f, BodyArchetype.MITE)
                        .withDescription("Tiny, quick and hard to aim at, and dies to almost any hit. Sent in swarms.")
                        .withStat(EnemyStat.AGILITY, 10f), RankGrowth.of(1.8f, 1))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Tiny, quick and slippery: it dodges a fair share of aimed shots. Sent in "
                                + "swarms.")
                        .withStat(EnemyStat.AGILITY, 25f))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("Tiny, quick and slippery, and bursts into three broodlings when it dies. "
                                + "Sent in swarms.")
                        .withAdditionalAbilities(List.of(IdentifiedAbility.anonymous(new Ability(new OnDeathTrigger(),
                                new SpawnEnemiesAction("miteBrood", AbilitySpawnShape.brood(3, 1f, 1f), false))))))
                .build());
        catalog.register(EnemyDefinition.of("miteBrood", "Mite Broodling", 30, 1, 1.7f, BodyArchetype.MITE)
                .withDescription("A broodling burst from a dying Mite boss. Tiny, quick and slippery.")
                .withStat(EnemyStat.AGILITY, 25f));
    }
}
