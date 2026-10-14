package td.enemy;

import td.effect.ShieldTemplate;
import td.stat.DisruptionAura;

import java.util.List;

/**
 * The Jammer ({@code j}): slows and blinds every tower near it, its casing shrugs off magic, and it jams wider and
 * harder with rank; from Elite its casing blunts a little of every magic hit and it keeps a static charge that soaks
 * magic hits.
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
                        .withMagicResist(20f)
                        .withDisruption(new DisruptionAura(100f, 0.3f, 0.6f)))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(432, 11))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(778, 16)
                        .withDescription("Jams every tower in a wider reach: they fire more slowly and see less far "
                                + "while it's in range. Its casing shrugs off some magic.")
                        .withDisruption(new DisruptionAura(115f, 0.35f, 0.7f)))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(1400, 31)
                        .withDescription("Jams every tower in a wider reach and harder: they fire more slowly and see "
                                + "less far while it's in range. Its casing shrugs off magic and blunts a little of "
                                + "every magic hit, and every few seconds it gathers a static charge that soaks magic "
                                + "hits.")
                        .withStats(10f, 30f, 0f, 2f, 15f, 5f, 10f)
                        .withDisruption(new DisruptionAura(125f, 0.4f, 0.8f))
                        .withAdditionalAbilities(List.of(staticCharge(120f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(2519, 56)
                        .withStats(15f, 40f, 0f, 4f, 25f, 5f, 20f)
                        .withDisruption(new DisruptionAura(140f, 0.5f, 1f))
                        .withAdditionalAbilities(List.of(staticCharge(240f))))
                .build());
    }

    private static IdentifiedAbility staticCharge(float points) {
        return IdentifiedAbility.named("staticCharge", new Ability(new PeriodicTrigger(100),
                new ApplyEffectAction(ShieldTemplate.magicOnly(points, 80), new SelfTarget())));
    }
}
