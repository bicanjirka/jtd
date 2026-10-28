package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Golem ({@code golem}): a slow stone hulk with heavy armor. It shrugs off crits, and once badly hurt it hardens
 * into a shield that holds until broken and grows with rank. From Elite it is plated and tougher against magic too.
 * When it dies it splits into two smaller Golemites with its defences and little health, a definition of their own
 * because a golem that spawns golems would be a cycle.
 */
final class GolemEnemy {

    // Shared, so a Golemite keeps the same share of its Golem's health at every rank.
    private static final RankGrowth GROWTH = RankGrowth.of(20f);

    private GolemEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("golemite", "Golemite", 55, 1, 0.9f, BodyArchetype.GOLEM)
                        .withDescription("A chunk of a broken Golem: as hard, but far easier to break.")
                        .withMovement(new SwingMovement((float) Math.toRadians(15.0), 30, SwingCurve.SMOOTH))
                        .withArmor(35f)
                        .withResilience(40f), GROWTH)
                .thenAt(Rank.ELITE, e -> e
                        .withStats(50f, 20f, 2f, 0f, 60f, 0f, 0f))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(65f, 35f, 4f, 0f, 80f, 0f, 0f))
                .build());
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("golem", "Golem", 220, 4, 0.7f, BodyArchetype.GOLEM)
                        .withDescription("Slow, stone-skinned and hard to crack.")
                        .withMovement(new SwingMovement((float) Math.toRadians(15.0), 50, SwingCurve.SMOOTH))
                        .withArmor(35f)
                        .withResilience(40f)
                        .withIdentifiedAbilities(List.of(stoneShield(40), split())), GROWTH)
                .thenAt(Rank.SOLDIER, e -> e
                        .withAdditionalAbilities(List.of(stoneShield(80))))
                .thenAt(Rank.VETERAN, e -> e
                        .withAdditionalAbilities(List.of(stoneShield(160))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(50f, 20f, 2f, 0f, 60f, 0f, 0f)
                        .withAdditionalAbilities(List.of(stoneShield(300))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(65f, 35f, 4f, 0f, 80f, 0f, 0f)
                        .withAdditionalAbilities(List.of(stoneShield(600))))
                .build());
    }

    /** Once badly hurt: a shield around itself that only damage takes down, so it has to be broken. */
    private static IdentifiedAbility stoneShield(float points) {
        return IdentifiedAbility.named("stoneShield", new Ability(AbilityTerm.STONE_SKIN, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.untilBroken(points), new SelfTarget())));
    }

    private static IdentifiedAbility split() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.SPLIT, new OnDeathTrigger(),
                new SpawnEnemiesAction("golemite", AbilitySpawnShape.brood(2, 1f, 1f), false)));
    }
}
