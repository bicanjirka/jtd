package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Golem ({@code golem}): a slow stone hulk with heavy armor. It shrugs off crits, and once hurt it hardens into a
 * shield that holds until broken and grows with rank. From Soldier it hardens a second time once badly hurt; from
 * Veteran, when it dies it splits into two smaller Golemites with its defences and little health, a definition of their
 * own because a golem that spawns golems would be a cycle. From Elite it is plated and tougher against magic too, but
 * too heavy to step aside from a crit or shake off what slows it, and so are its Golemites, which also harden once,
 * badly hurt.
 */
final class GolemEnemy {

    private GolemEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("golemite", "Golemite", 50, 1, 0.9f, BodyArchetype.GOLEM)
                                .withDescription("A chunk of a broken Golem: as hard, but far easier to break.")
                                .withMovement(new SwingMovement((float) Math.toRadians(15.0), 30, SwingCurve.SMOOTH))
                                .withArmor(35f)
                                .withResilience(40f),
                        RankGrowth.of(18f))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(50f, 20f, 5f, 0f, 0f, -20f, 60f, 0f)
                        .withAdditionalAbilities(List.of(lastStoneSkin(250))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(65f, 35f, 10f, 0f, 0f, -40f, 80f, 0f))
                .build());
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("golem", "Golem", 222, 3, 0.65f, BodyArchetype.GOLEM)
                                .withDescription("Slow, stone-skinned and hard to crack.")
                                .withMovement(new SwingMovement((float) Math.toRadians(25.0), 50, SwingCurve.SMOOTH))
                                .withArmor(35f)
                                .withResilience(40f)
                                .withIdentifiedAbilities(List.of(stoneSkin(80))),
                        RankGrowth.stepped(359, 818, 2826, 6391).paying(5, 5, 11, 22))
                .thenAt(Rank.SOLDIER, e -> e
                        .withArmor(45f)
                        .withResilience(50f)
                        .withAdditionalAbilities(List.of(stoneSkin(80), lastStoneSkin(80))))
                .thenAt(Rank.VETERAN, e -> e
                        .withAdditionalAbilities(List.of(stoneSkin(160), lastStoneSkin(240), split())))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(80f, 20f, 2f, 0f, -20f, -20f, 60f, 0f)
                        .withAdditionalAbilities(List.of(stoneSkin(320), lastStoneSkin(480))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(110f, 35f, 6f, 0f, -40f, -20f, 80f, 0f)
                        .withAdditionalAbilities(List.of(stoneSkin(1200), lastStoneSkin(1600))))
                .build());
    }

    /** Once hurt: a shield around itself that only damage takes down, so it has to be broken. */
    private static IdentifiedAbility stoneSkin(float points) {
        return IdentifiedAbility.named("stoneSkin", new Ability(AbilityTerm.STONE_SKIN, new HealthThresholdTrigger(0.666f),
                new ApplyEffectAction(ShieldTemplate.untilBroken(points), new SelfTarget())));
    }

    /** The same once badly hurt, the last layer of stone. */
    private static IdentifiedAbility lastStoneSkin(float points) {
        return IdentifiedAbility.named("lastStoneSkin", new Ability(AbilityTerm.STONE_SKIN, new HealthThresholdTrigger(0.333f),
                new ApplyEffectAction(ShieldTemplate.untilBroken(points), new SelfTarget())));
    }

    private static IdentifiedAbility split() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.SPLIT, new OnDeathTrigger(),
                new SpawnEnemiesAction("golemite", AbilitySpawnShape.brood(2, 1f, 1f), false)));
    }
}
