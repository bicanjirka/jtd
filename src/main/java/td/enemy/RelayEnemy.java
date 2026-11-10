package td.enemy;

import java.util.List;

/**
 * The Relay ({@code relay}): a fragile signal post that calls Walkers of its rank the first time it is hurt, and again
 * once below half health from Veteran; it calls more from Elite. Kill it in one blow, or fight what it calls.
 */
final class RelayEnemy {

    private RelayEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("relay", "Relay", 70, 3, 1.1f, BodyArchetype.RELAY)
                        .withDescription("Calls for help when hurt; kill it in one blow.")
                        .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                        .withMagicResist(10f)
                        .withIdentifiedAbilities(List.of(alarm(2))), RankGrowth.of(14f))
                .thenAt(Rank.VETERAN, e -> e
                        .withAdditionalAbilities(List.of(distress(2))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 20f, 0f, 0f, 10f, 10f, 10f)
                        .withAdditionalAbilities(List.of(alarm(3), distress(3))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 30f, 0f, 0f, 20f, 15f, 20f)
                        .withAdditionalAbilities(List.of(alarm(4), distress(4))))
                .build());
    }

    /** The call it sends the first time it is hurt. */
    private static IdentifiedAbility alarm(int walkers) {
        return IdentifiedAbility.named("alarm", new Ability(AbilityTerm.REINFORCE, new OnFirstDamageTakenTrigger(),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1f, 1f), false)));
    }

    /** The call it sends once badly hurt. */
    private static IdentifiedAbility distress(int walkers) {
        return IdentifiedAbility.named("distress", new Ability(AbilityTerm.REINFORCE, new HealthThresholdTrigger(0.5f),
                new SpawnEnemiesAction("walker", AbilitySpawnShape.brood(walkers, 1f, 1f), false)));
    }
}
