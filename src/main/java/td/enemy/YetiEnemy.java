package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;

import java.util.List;

/**
 * The Yeti ({@code yeti}): immune to freeze and half as chilled, so a Seeker-only defence fails; Dazed still stops
 * it. Its fur mitigates both damage types; it shakes off chill more with rank, from Elite the fur blunts a little of
 * every hit and it hunkers down and heals once badly hurt, and at Boss crits glance off its hide.
 */
final class YetiEnemy {

    private YetiEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("yeti", "Yeti", 220, 6, 1.1f, BodyArchetype.YETI)
                        .withDescription("Immune to freeze, and chills half as long. Its thick fur blunts some of "
                                + "every hit. Dazed still stops it.")
                        .withArmor(15f)
                        .withMagicResist(15f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.FREEZE)),
                                IdentifiedTrait.named("chill", EffectResistTrait.resisting(EffectKind.CHILL, 0.5f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(440, 11))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(880, 16)
                        .withDescription("Immune to freeze, and chills only a quarter as long. Its thick fur blunts "
                                + "some of every hit. Dazed still stops it.")
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("chill", EffectResistTrait.resisting(EffectKind.CHILL, 0.75f)))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(1760, 31)
                        .withDescription("Immune to freeze, and chills only a quarter as long. Its thick fur blunts "
                                + "some of every hit, and once badly hurt it hunkers down and heals. Dazed still "
                                + "stops it.")
                        .withStats(20f, 20f, 2f, 2f, 20f, 5f, 15f)
                        .withAdditionalAbilities(List.of(hunker(6f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(3520, 56)
                        .withDescription("Immune to freeze, and chills only a quarter as long. Its thick fur blunts "
                                + "some of every hit and crits glance off its hide; once badly hurt it hunkers down "
                                + "and heals. Dazed still stops it.")
                        .withStats(25f, 25f, 3f, 3f, 50f, 5f, 25f)
                        .withAdditionalAbilities(List.of(hunker(12f))))
                .build());
    }

    private static IdentifiedAbility hunker(float perTick) {
        return IdentifiedAbility.named("hunker", new Ability(AbilityTerm.HUNKER, new HealthThresholdTrigger(0.4f),
                new ApplyEffectAction(new HealTemplate(perTick, 60), new SelfTarget())));
    }
}
