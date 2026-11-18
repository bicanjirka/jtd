package td.enemy;

import td.effect.ShieldTemplate;

import java.util.List;

/**
 * The Templar ({@code templar}): magic barely touches it, and it has little armor, so physical damage handles it
 * nearly as it handles a Walker. From Soldier its ward also blunts every small magic hit; from Veteran it slowly heals
 * and raises a shield against magic, holding until broken, once badly hurt.
 */
final class TemplarEnemy {

    private TemplarEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("templar", "Templar", 114, 3, 1.28f, BodyArchetype.TEMPLAR)
                        .withDescription("Spells slide off its ward; a blade does not.")
                        .withMovement(new SwingMovement((float) Math.toRadians(6.0), 40, SwingCurve.SMOOTH))
                        .withMagicResist(50f),
                        RankGrowth.stepped(179, 502, 1367, 3349).paying(5, 8, 16, 26))
                .thenAt(Rank.SOLDIER, e -> e
                        .withMagicResist(70f)
                        .withInsulation(5f))
                .thenAt(Rank.VETERAN, e -> e
                        .withStats(10f, 120f, 0f, 10f, 0f, 15f, 0f, 15f)
                        .withAdditionalAbilities(List.of(bulwark(240f)))
                        .withAdditionalTraits(List.of(regeneration(4f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(20f, 160f, 0f, 10f, 0f, 20f, 10f, 30f)
                        .withAdditionalAbilities(List.of(bulwark(480f)))
                        .withAdditionalTraits(List.of(regeneration(8f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(30f, 220f, 0f, 25f, 0f, 25f, 20f, 45f)
                        .withAdditionalAbilities(List.of(bulwark(880f)))
                        .withAdditionalTraits(List.of(regeneration(16f))))
                .build());
    }

    private static IdentifiedTrait regeneration(float pointsPerSecond) {
        return IdentifiedTrait.named("regeneration", new RegenerationTrait(pointsPerSecond));
    }

    /** The shield against magic it raises once badly hurt. */
    private static IdentifiedAbility bulwark(float points) {
        return IdentifiedAbility.named("bulwark", new Ability(AbilityTerm.BULWARK, new HealthThresholdTrigger(0.333f),
                new ApplyEffectAction(ShieldTemplate.magicOnlyUntilBroken(points), new SelfTarget())));
    }
}
