package td.enemy;

import td.effect.ShieldTemplate;
import td.stat.DisruptionAura;
import td.stat.EnemyStat;

import java.util.List;

/**
 * The Jammer ({@code j}): slows and blinds every tower near it, shrugs off some magic, and jams wider and harder with
 * rank; from Elite it keeps a static charge that soaks magic hits.
 */
final class JammerEnemy {

    private JammerEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("j", "Jammer", 240, 6, 1.0f, BodyArchetype.SQUARE)
                        .withDescription("Jams every tower near it: they fire more slowly and see less far while it's "
                                + "in range. Its casing shrugs off some magic.")
                        .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                        .withStat(EnemyStat.MAGIC_RESIST, 20f)
                        .withDisruption(new DisruptionAura(100f, 0.3f, 0.6f)), RankGrowth.of(1.8f, 5))
                .thenAt(Rank.VETERAN, e -> e
                        .withDescription("Jams every tower in a wider reach: they fire more slowly and see less far "
                                + "while it's in range. Its casing shrugs off some magic.")
                        .withDisruption(new DisruptionAura(115f, 0.35f, 0.7f)))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Jams every tower in a wider reach and harder: they fire more slowly and see "
                                + "less far while it's in range. Its casing shrugs off some magic, and every few "
                                + "seconds it gathers a static charge that soaks magic hits.")
                        .withDisruption(new DisruptionAura(125f, 0.4f, 0.8f))
                        .withAdditionalAbilities(List.of(staticCharge(120f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStat(EnemyStat.MAGIC_RESIST, 40f)
                        .withDisruption(new DisruptionAura(140f, 0.5f, 1f))
                        .withAdditionalAbilities(List.of(staticCharge(240f))))
                .build());
    }

    private static IdentifiedAbility staticCharge(float points) {
        return IdentifiedAbility.named("staticCharge", new Ability(new PeriodicTrigger(100),
                new ApplyEffectAction(ShieldTemplate.magicOnly(points, 80), new SelfTarget())));
    }
}
