package td.enemy;

import java.util.List;

/**
 * The Relay ({@code relay}): a signal post that calls a Walker a rank below its own (a Grunt at Grunt) the first time
 * it is hurt, and from Veteran another once below half health; it calls more from Elite. It is heavily insulated, more
 * with rank, so a burn or any other small magic hit does next to nothing to it. Kill it in one blow, or fight what it
 * calls.
 */
final class RelayEnemy {

    private RelayEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("relay", "Relay", 59, 1, 1.1f, BodyArchetype.RELAY)
                        .withDescription("Calls for help when hurt; kill it in one blow.")
                        .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                        .withInsulation(15f)
                        .withIdentifiedAbilities(List.of(alarm(1, Rank.GRUNT))), RankGrowth.stepped(128, 405, 1184, 2731).paying(3, 3, 3, 3))
                .thenAt(Rank.SOLDIER, e -> e
                        .withInsulation(20f)
                        .withAdditionalAbilities(List.of(alarm(1, Rank.GRUNT))))
                .thenAt(Rank.VETERAN, e -> e
                        .withInsulation(25f)
                        .withAdditionalAbilities(List.of(alarm(1, Rank.SOLDIER), distress(1, Rank.SOLDIER))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 0f, 30f, 10f, 10f, 10f)
                        .withAdditionalAbilities(List.of(alarm(2, Rank.VETERAN), distress(1, Rank.VETERAN))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 15f, 0f, 40f, 20f, 15f, 20f)
                        .withAdditionalAbilities(List.of(alarm(2, Rank.ELITE), distress(2, Rank.ELITE))))
                .build());
    }

    /** The call it sends the first time it is hurt; what it calls is weaker than itself, so its body carries its weight. */
    private static IdentifiedAbility alarm(int walkers, Rank rank) {
        return IdentifiedAbility.named("alarm", new Ability(AbilityTerm.REINFORCE, new OnFirstDamageTakenTrigger(),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1f, 1f), false).atRank(rank)));
    }

    /** The call it sends once badly hurt. */
    private static IdentifiedAbility distress(int walkers, Rank rank) {
        return IdentifiedAbility.named("distress", new Ability(AbilityTerm.REINFORCE, new HealthThresholdTrigger(0.5f),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1f, 1f), false).atRank(rank)));
    }
}
