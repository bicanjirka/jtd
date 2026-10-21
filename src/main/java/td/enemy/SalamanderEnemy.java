package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;

import java.util.List;

/**
 * The Salamander ({@code salamander}): immune to burn, so a Cinder-only defence does nothing; Soulfire still burns
 * it. It regrows what it loses, faster with rank; from Elite its hide turns some magic and it sheds its wounds once
 * badly hurt.
 */
final class SalamanderEnemy {

    private SalamanderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("salamander", "Salamander", 180, 5, 1.28f, BodyArchetype.SALAMANDER)
                        .withDescription("Immune to burn, and slowly regrows what it loses. Soulfire still burns it.")
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)),
                                regrowth(1f))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(342, 9))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(650, 13).withAdditionalTraits(List.of(regrowth(2f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(1235, 25)
                        .withDescription("Immune to burn, regrows what it loses, its hide turns some magic, and once "
                                + "badly hurt it sheds its wounds in a burst of healing. Soulfire still burns it.")
                        .withStats(10f, 25f, 0f, 0f, 10f, 10f, 15f)
                        .withAdditionalTraits(List.of(regrowth(4f)))
                        .withAdditionalAbilities(List.of(shed(4f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(2346, 45)
                        .withStats(15f, 30f, 0f, 0f, 20f, 10f, 25f)
                        .withAdditionalTraits(List.of(regrowth(8f)))
                        .withAdditionalAbilities(List.of(shed(8f))))
                .build());
    }

    private static IdentifiedTrait regrowth(float pointsPerSecond) {
        return IdentifiedTrait.named("regrowth", new RegenerationTrait(pointsPerSecond));
    }

    private static IdentifiedAbility shed(float perTick) {
        return IdentifiedAbility.named("shed", new Ability(AbilityTerm.SHED_SKIN, new HealthThresholdTrigger(0.3f),
                new ApplyEffectAction(new HealTemplate(perTick, 60), new SelfTarget())));
    }
}
