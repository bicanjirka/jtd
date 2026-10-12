package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/** The Shieldbearer ({@code shieldbearer}): gives the allies near it a physical-only shield; magic ignores it. */
final class ShieldbearerEnemy {

    private ShieldbearerEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("shieldbearer", "Shieldbearer", 70, 6, 1.0f, BodyArchetype.SHIELDBEARER)
                        // "other" is load-bearing: a radius target excludes the caster.
                        .withDescription("Periodically shields every other ally near it against physical damage. "
                                + "Magic goes straight through.")
                        .withIdentifiedAbilities(List.of(shield(20f))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(140, 15).withAdditionalAbilities(List.of(shield(40f))))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(280, 38).withAdditionalAbilities(List.of(shield(80f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(560, 94).withAdditionalAbilities(List.of(shield(160f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1120, 235).withAdditionalAbilities(List.of(shield(320f))))
                .build());
    }

    /** The physical-only shield of {@code points} every other ally within reach gets. */
    private static IdentifiedAbility shield(float points) {
        return IdentifiedAbility.named("shield", new Ability(new PeriodicTrigger(100),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 60), new RadiusTarget(100f))));
    }
}
