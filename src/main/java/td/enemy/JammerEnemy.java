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
                .startingAt(EnemyDefinition.of("jammer", "Jammer", 180, 3, 1.0f, BodyArchetype.SQUARE)
                        .withDescription("Weakens every tower it walks past; kill it first.")
                        .withMovement(new RotorMovement((float) Math.toRadians(2.0)))
                        .withMagicResist(20f)
                        .withDisruption(new DisruptionAura(100f, 0.3f, 0.6f)))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(324, 6))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(584, 8)
                        .withDisruption(new DisruptionAura(115f, 0.35f, 0.7f)))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(1050, 16)
                        .withStats(10f, 30f, 0f, 2f, 15f, 5f, 10f)
                        .withDisruption(new DisruptionAura(125f, 0.4f, 0.8f))
                        .withAdditionalAbilities(List.of(staticCharge(120f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1889, 28)
                        .withStats(15f, 40f, 0f, 4f, 25f, 5f, 20f)
                        .withDisruption(new DisruptionAura(140f, 0.5f, 1f))
                        .withAdditionalAbilities(List.of(staticCharge(240f))))
                .build());
    }

    private static IdentifiedAbility staticCharge(float points) {
        return IdentifiedAbility.named("staticCharge", new Ability(AbilityTerm.CAPACITOR, new PeriodicTrigger(100),
                new ApplyEffectAction(ShieldTemplate.magicOnly(points, 80), new SelfTarget())));
    }
}
