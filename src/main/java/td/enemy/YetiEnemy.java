package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;

import java.util.List;

/**
 * The Yeti ({@code yeti}): immune to freeze and chill, so a Seeker-only defence fails; Dazed still stops
 * it. Its fur turns blows far better than magic, more with rank; from Soldier it takes heals well; from Veteran the fur
 * blunts a little of every hit, it shakes off what slows it sooner, crits glance off it more and it hunkers down and
 * heals once badly hurt.
 */
final class YetiEnemy {

    private YetiEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("yeti", "Yeti", 125, 3, 1.1f, BodyArchetype.YETI)
                        .withMovement(new SwingMovement((float) Math.toRadians(15.0), 30, SwingCurve.SMOOTH))
                        .withDescription("Thick fur against cold and blows alike.")
                        .withArmor(15f)
                        .withMagicResist(5f)
                        .withTraits(List.of(
                                new ImmunityTrait(EffectKind.FREEZE),
                                new ImmunityTrait(EffectKind.CHILL))),
                        RankGrowth.stepped(204, 636, 1250, 3256).paying(5, 9, 17, 28))
                .thenAt(Rank.SOLDIER, e -> e
                        .withArmor(25f)
                        .withMagicResist(10f)
                        .withSpirit(25f))
                .thenAt(Rank.VETERAN, e -> e
                        .withStats(40f, 20f, 2f, 2f, 0f, 10f, 20f, 35f)
                        .withAdditionalAbilities(List.of(hunker(2f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(65f, 20f, 2f, 2f, 0f, 20f, 25f, 60f)
                        .withAdditionalAbilities(List.of(hunker(8f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(100f, 25f, 3f, 3f, 0f, 33f, 50f, 75f)
                        .withAdditionalAbilities(List.of(hunker(32f))))
                .build());
    }

    private static IdentifiedAbility hunker(float perTick) {
        return IdentifiedAbility.named("hunker", new Ability(AbilityTerm.HUNKER, new HealthThresholdTrigger(0.4f),
                new ApplyEffectAction(new HealTemplate(perTick, 60), new SelfTarget())));
    }
}
