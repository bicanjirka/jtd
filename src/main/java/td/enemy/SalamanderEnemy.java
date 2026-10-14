package td.enemy;

import td.effect.EffectKind;
import td.effect.HealTemplate;
import td.stat.EnemyStat;

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
                        // Regeneration is in damage units a tick: 5 is one point a second.
                        .withStat(EnemyStat.REGENERATION, 5f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.anonymous(EffectResistTrait.immuneTo(EffectKind.BURN)))),
                        RankGrowth.of(1.9f, 4))
                .thenAt(Rank.VETERAN, e -> e.withStat(EnemyStat.REGENERATION, 10f))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Immune to burn, regrows what it loses, its hide turns some magic, and once "
                                + "badly hurt it sheds its wounds in a burst of healing. Soulfire still burns it.")
                        .withStat(EnemyStat.REGENERATION, 20f)
                        .withStat(EnemyStat.MAGIC_RESIST, 25f)
                        .withAdditionalAbilities(List.of(shed(4f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStat(EnemyStat.REGENERATION, 40f)
                        .withAdditionalAbilities(List.of(shed(8f))))
                .build());
    }

    private static IdentifiedAbility shed(float perTick) {
        return IdentifiedAbility.named("shed", new Ability(new HealthThresholdTrigger(0.3f),
                new ApplyEffectAction(new HealTemplate(perTick, 60), new SelfTarget())));
    }
}
