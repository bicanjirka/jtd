package td.enemy;

import java.util.List;

/**
 * The Relay ({@code relay}): a signal post that calls Walkers a rank below its own (Grunts at Grunt and Soldier) the
 * first time it is hurt, more with rank and unpaid from Elite, and from Veteran a second, unpaid call of tougher Walkers
 * once badly hurt. It
 * is heavily insulated, more with rank, so a burn or any other small magic hit does next to nothing to it. Kill it in
 * one blow, or fight what it calls.
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
                                .withIdentifiedAbilities(List.of(alarm(1, Rank.GRUNT))),
                        RankGrowth.stepped(128, 405, 1184, 2731).paying(2, 2, 3, 3))
                .thenAt(Rank.SOLDIER, e -> e
                        .withInsulation(20f)
                        .withAdditionalAbilities(List.of(alarm(2, Rank.GRUNT))))
                .thenAt(Rank.VETERAN, e -> e
                        .withInsulation(30f)
                        .withAdditionalAbilities(List.of(alarm(3, Rank.SOLDIER), distress(3, Rank.SOLDIER))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 5f, 50f, 10f, 0f, 20f, 15f)
                        .withAdditionalAbilities(List.of(unpaidAlarm(5, Rank.VETERAN), distress(4, Rank.VETERAN))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(25f, 25f, 5f, 100f, 15f, 0f, 40f, 35f)
                        .withAdditionalAbilities(List.of(unpaidAlarm(10, Rank.VETERAN), distress(5, Rank.ELITE))))
                .build());
    }

    /** The call it sends the first time it is hurt; what it calls is weaker than itself, so its body carries its weight. */
    private static IdentifiedAbility alarm(int walkers, Rank rank) {
        return IdentifiedAbility.named("alarm", new Ability(AbilityTerm.REINFORCE, new OnFirstDamageTakenTrigger(),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1f, 1f), false).atRank(rank)));
    }

    /** The same call from Elite on, paying nothing, since a large call that pays would outpay the Relay many times. */
    private static IdentifiedAbility unpaidAlarm(int walkers, Rank rank) {
        return IdentifiedAbility.named("alarm", new Ability(AbilityTerm.REINFORCE, new OnFirstDamageTakenTrigger(),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1f, 0f), false).atRank(rank)));
    }

    /** The call it sends once badly hurt: Walkers tougher than their rank, paying nothing. */
    private static IdentifiedAbility distress(int walkers, Rank rank) {
        return IdentifiedAbility.named("distress", new Ability(AbilityTerm.REINFORCE, new HealthThresholdTrigger(0.25f),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1.5f, 0f), false).atRank(rank)));
    }
}
