package td.enemy;

import java.util.List;

/**
 * The Relay ({@code relay}): a fragile signal post that calls a Walker of its rank the first time it is hurt, and from
 * Veteran another once below half health; it calls more from Elite. It is insulated, more with rank, so a burn or any
 * other small magic hit does next to nothing to it. Kill it in one blow, or fight what it calls.
 */
final class RelayEnemy {

    private RelayEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("relay", "Relay", 72, 1, 1.1f, BodyArchetype.RELAY)
                        .withDescription("Calls for help when hurt; kill it in one blow.")
                        .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                        .withInsulation(3f)
                        .withIdentifiedAbilities(List.of(alarm(1))), RankGrowth.stepped(109, 150, 534, 1178).paying(2, 2, 2, 2))
                .thenAt(Rank.SOLDIER, e -> e.withInsulation(4f))
                .thenAt(Rank.VETERAN, e -> e
                        .withInsulation(6f)
                        .withAdditionalAbilities(List.of(distress(1))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 0f, 9f, 10f, 10f, 10f)
                        .withAdditionalAbilities(List.of(alarm(2), distress(1))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 15f, 0f, 12f, 20f, 15f, 20f)
                        .withAdditionalAbilities(List.of(alarm(2), distress(2))))
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
