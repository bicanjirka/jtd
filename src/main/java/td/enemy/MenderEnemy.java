package td.enemy;

import td.effect.HealTemplate;

import java.util.List;

/**
 * The Mender mob ({@code m}): heals the allies near it, takes heals well itself (its spirit grows with rank), from
 * Veteran heals itself when left alone, and at Boss leaves a last blessing on the allies around it when it dies.
 */
final class MenderEnemy {

    private MenderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("m", "Mender mob", 120, 5, 1.28f, BodyArchetype.MENDER)
                        // "other" is load-bearing: a radius target excludes the caster.
                        .withDescription("Keeps the rest of its wave on its feet.")
                        .withSpirit(20f)
                        .withIdentifiedAbilities(List.of(heal(0.25f, 40))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(216, 9)
                        .withAdditionalAbilities(List.of(heal(0.5f, 40))))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(389, 13)
                        .withSpirit(30f)
                        .withAdditionalAbilities(List.of(heal(1f, 40), selfHeal(0.7f))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(700, 25)
                        .withStats(10f, 10f, 0f, 0f, 10f, 5f, 40f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(67f))))
                        .withAdditionalAbilities(List.of(heal(2f, 80), selfHeal(1.4f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1260, 45)
                        .withStats(15f, 15f, 0f, 0f, 20f, 5f, 60f)
                        .withAdditionalAbilities(List.of(heal(4f, 80), selfHeal(2.8f),
                                IdentifiedAbility.anonymous(new Ability(AbilityTerm.LAST_RITES, new OnDeathTrigger(),
                                        new ApplyEffectAction(new HealTemplate(4f, 100), new RadiusTarget(120f)))))))
                .build());
    }

    /** The pulse every other ally within reach gets each second. */
    private static IdentifiedAbility heal(float perTick, int durationTicks) {
        return IdentifiedAbility.named("heal", new Ability(AbilityTerm.MEND, new PeriodicTrigger(20),
                new ApplyEffectAction(new HealTemplate(perTick, durationTicks), new RadiusTarget(80f))));
    }

    private static IdentifiedAbility selfHeal(float perTick) {
        return IdentifiedAbility.named("selfHeal", new Ability(AbilityTerm.RECOVER, new TimeSinceLastHitTrigger(100),
                new ApplyEffectAction(new HealTemplate(perTick, 100), new SelfTarget())));
    }
}
