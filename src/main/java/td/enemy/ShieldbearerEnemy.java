package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Shieldbearer ({@code shieldbearer}): gives the allies near it a physical-only shield, and carries armor of its
 * own; magic ignores both. Its spirit makes the shields it raises on itself stronger. From Elite it raises a shield
 * for itself too and its own shield blunts a little of every physical hit, and at Boss its reach widens.
 */
final class ShieldbearerEnemy {

    private ShieldbearerEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("shield", "Shieldbearer", 105, 3, 1.0f, BodyArchetype.SHIELDBEARER)
                        // "other" is load-bearing: a radius target excludes the caster.
                        .withDescription("Guards the enemies around it; break it first.")
                        .withArmor(30f)
                        .withIdentifiedAbilities(List.of(shield(40f, 100f))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(189, 6)
                        .withAdditionalAbilities(List.of(shield(80f, 100f))))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(341, 8)
                        .withSpirit(10f)
                        .withAdditionalAbilities(List.of(shield(160f, 100f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(612, 16)
                        .withStats(40f, 10f, 2f, 0f, 20f, 5f, 20f)
                        .withAdditionalAbilities(List.of(shield(320f, 100f), selfShield(320f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1103, 28)
                        .withStats(50f, 15f, 4f, 0f, 30f, 5f, 30f)
                        .withAdditionalAbilities(List.of(shield(640f, 130f), selfShield(640f))))
                .build());
    }

    /** The physical-only shield of {@code points} every other ally within {@code radius} gets. */
    private static IdentifiedAbility shield(float points, float radius) {
        return IdentifiedAbility.named("shield", new Ability(AbilityTerm.SHIELD_WALL, new PeriodicTrigger(100),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 60), new RadiusTarget(radius))));
    }

    private static IdentifiedAbility selfShield(float points) {
        return IdentifiedAbility.named("selfShield", new Ability(AbilityTerm.GUARD, new PeriodicTrigger(100),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 60), new SelfTarget())));
    }
}
