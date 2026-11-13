package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Templar ({@code templar}): magic barely touches it, and it has no armor, so physical damage handles it as it
 * handles a Walker. From Veteran it raises a shield against magic once badly hurt.
 */
final class TemplarEnemy {

    private TemplarEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("templar", "Templar", 114, 3, 1.28f, BodyArchetype.TEMPLAR)
                        .withDescription("Spells slide off its ward; a blade does not.")
                        .withMovement(new SwingMovement((float) Math.toRadians(6.0), 40, SwingCurve.SMOOTH))
                        .withMagicResist(70f),
                        RankGrowth.stepped(179, 502, 1367, 3349).paying(5, 8, 16, 26))
                .thenAt(Rank.SOLDIER, e -> e.withMagicResist(90f))
                .thenAt(Rank.VETERAN, e -> e
                        .withMagicResist(110f)
                        .withAdditionalAbilities(List.of(bulwark(100f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(5f, 130f, 0f, 5f, 10f, 0f, 10f)
                        .withAdditionalAbilities(List.of(bulwark(250f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(10f, 200f, 0f, 10f, 20f, 0f, 20f)
                        .withAdditionalAbilities(List.of(bulwark(600f))))
                .build());
    }

    /** The shield against magic it raises once, at half health. */
    private static IdentifiedAbility bulwark(float points) {
        return IdentifiedAbility.named("bulwark", new Ability(AbilityTerm.BULWARK, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.magicOnly(points, 100), new SelfTarget())));
    }
}
