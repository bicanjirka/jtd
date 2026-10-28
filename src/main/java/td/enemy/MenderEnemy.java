package td.enemy;

import td.effect.HealTemplate;
import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Mender mob ({@code m}): heals the allies near it, takes heals well itself (its spirit grows with rank), from
 * Veteran heals itself when left alone and leaves a last
 * blessing of healing and a shield on the allies around it when it dies; at Boss the blessing is a heal alone.
 */
final class MenderEnemy {

    private static final float BLESSING_RADIUS = 120f;

    private MenderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("mender", "Mender mob", 90, 3, 1.28f, BodyArchetype.MENDER)
                        // "other" is load-bearing: a radius target excludes the caster.
                        .withDescription("Keeps the rest of its wave on its feet.")
                        .withMovement(new SwingMovement())
                        .withSpirit(20f)
                        .withIdentifiedAbilities(List.of(heal(0.25f, 40))), RankGrowth.of(14f))
                .thenAt(Rank.SOLDIER, e -> e
                        .withAdditionalAbilities(List.of(heal(0.5f, 40))))
                .thenAt(Rank.VETERAN, e -> e
                        .withSpirit(30f)
                        .withAdditionalAbilities(List.of(heal(1f, 40), selfHeal(0.7f), lastRites(2f, 40),
                                lastWard(60f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 0f, 0f, 10f, 5f, 40f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(67f))))
                        .withAdditionalAbilities(List.of(heal(2f, 80), selfHeal(1.4f), lastRites(3f, 40),
                                lastWard(150f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 15f, 0f, 0f, 20f, 5f, 60f)
                        .withoutAbility("lastWard")
                        .withAdditionalAbilities(List.of(heal(4f, 80), selfHeal(2.8f), lastRites(4f, 100))))
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

    /** The heal it leaves on the allies around it as it dies. */
    private static IdentifiedAbility lastRites(float perTick, int durationTicks) {
        return IdentifiedAbility.named("lastRites", new Ability(AbilityTerm.LAST_RITES, new OnDeathTrigger(),
                new ApplyEffectAction(new HealTemplate(perTick, durationTicks), new RadiusTarget(BLESSING_RADIUS))));
    }

    /** The shield it leaves on the allies around it as it dies. */
    private static IdentifiedAbility lastWard(float points) {
        return IdentifiedAbility.named("lastWard", new Ability(AbilityTerm.LAST_WARD, new OnDeathTrigger(),
                new ApplyEffectAction(new ShieldTemplate(points, 100), new RadiusTarget(BLESSING_RADIUS))));
    }
}
