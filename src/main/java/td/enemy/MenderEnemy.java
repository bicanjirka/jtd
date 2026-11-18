package td.enemy;

import td.effect.HealTemplate;
import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Mender ({@code mender}): heals the allies near it and takes heals well itself (its spirit grows with rank); from
 * Soldier it heals itself when left alone; from Veteran it leaves a last blessing of healing and a shield on the allies
 * around it when it dies; from Elite its robe adapts to whichever damage type the level deals most; at Boss the blessing
 * is a heal alone, and a far greater one.
 */
final class MenderEnemy {

    private static final float BLESSING_RADIUS = 120f;

    private MenderEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("mender", "Mender", 67, 3, 1.28f, BodyArchetype.MENDER)
                        // "other" is load-bearing: a radius target excludes the caster.
                        .withDescription("Keeps the rest of its wave on its feet.")
                        .withMovement(new SwingMovement())
                        .withSpirit(20f)
                        .withIdentifiedAbilities(List.of(heal(0.3f, 40))), RankGrowth.stepped(95, 230, 520, 1050).paying(5, 8, 17, 28))
                .thenAt(Rank.SOLDIER, e -> e
                        .withAdditionalAbilities(List.of(heal(0.4f, 40), selfHeal(2f))))
                .thenAt(Rank.VETERAN, e -> e
                        .withSpirit(50f)
                        .withAdditionalAbilities(List.of(heal(1f, 40), selfHeal(4f), lastRites(6f, 40),
                                lastWard(250f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(25f, 25f, 0f, 2f, 5f, 0f, 25f, 100f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(100f))))
                        .withAdditionalAbilities(List.of(heal(2f, 80), selfHeal(5f), lastRites(10f, 40),
                                lastWard(333f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(40f, 40f, 0f, 5f, 5f, 0f, 40f, 100f)
                        .withoutAbility("lastWard")
                        .withAdditionalAbilities(List.of(heal(3f, 80), selfHeal(10f), lastRites(12f, 100))))
                .build());
    }

    /** The pulse every other ally within reach gets each second. */
    private static IdentifiedAbility heal(float perTick, int durationTicks) {
        return IdentifiedAbility.named("heal", new Ability(AbilityTerm.MEND, new PeriodicTrigger(20),
                new ApplyEffectAction(new HealTemplate(perTick, durationTicks), new RadiusTarget(80f))));
    }

    /** Its own mending, which a single hit interrupts. */
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
