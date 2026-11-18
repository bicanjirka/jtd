package td.enemy;

import td.effect.ShieldTemplate;
import td.util.TickRate;

import java.util.List;

/**
 * The Knight ({@code knight}): armor and magic resist, plating, immune to crits, and from Veteran heavy enough to
 * shrug off some of a freeze. From Elite it braces against physical hits once badly hurt and lays mines along its path
 * that shield the first enemy to step on them. At Boss its suit adapts to whichever damage type the level deals most,
 * and it calls Squires to hold the line: a definition of their own, not the ladder's boss, because a boss that
 * reinforces with itself would be a spawn cycle.
 */
final class KnightEnemy {

    private KnightEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("knight", "Knight", 91, 3, 1.28f, BodyArchetype.SQUARE)
                        .withDescription("Heavy plates blunt physical hits; magic goes through.")
                        .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                        .withArmor(25f)
                        .withMagicResist(25f)
                        .withPlating(2f)
                        .withResilience(100f),
                        RankGrowth.stepped(157, 381, 808, 2100).paying(5, 8, 15, 15))
                .thenAt(Rank.SOLDIER, e -> e
                        .withArmor(33f)
                        .withMagicResist(33f))
                .thenAt(Rank.VETERAN, e -> e
                        .withArmor(53f)
                        .withMagicResist(53f)
                        .withPlating(3f)
                        .withTenacity(33f))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(100f, 100f, 8f, 0f, 5f, 33f, 100f, 10f)
                        .withAdditionalAbilities(List.of(brace(300f), wardMine(120f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(100f, 100f, 10f, 0f, 5f, 50f, 100f, 20f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.anonymous(AdaptiveResist.againstDominant(100f))))
                        .withAdditionalAbilities(List.of(brace(600f), wardMine(240f), callSquires())))
                .build());
        catalog.register(EnemyDefinition.of("squire", "Squire", 300, 1, 1.28f, BodyArchetype.SQUARE)
                .withDescription("Called in by a Knight boss to hold the line.")
                .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                .withArmor(35f)
                .withMagicResist(25f)
                .withPlating(8f));
    }

    /** Squires on a timer, so they pay nothing. */
    private static IdentifiedAbility callSquires() {
        return IdentifiedAbility.anonymous(new Ability(AbilityTerm.REINFORCE,
                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                new SpawnEnemiesAction("squire", AbilitySpawnShape.brood(3, 1f, 1f).unpaid(), false)));
    }

    /** A mine left on the path for a while, that shields the first enemy to step on it. */
    private static IdentifiedAbility wardMine(float shieldPoints) {
        return IdentifiedAbility.named("wardMine", new Ability(AbilityTerm.WARD_MINE,
                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                LeaveZoneAction.wardMine(0.5f, Math.round(8 * TickRate.TICKS_PER_SECOND), shieldPoints)));
    }

    /** The physical shield it raises once badly hurt. */
    private static IdentifiedAbility brace(float points) {
        return IdentifiedAbility.named("brace", new Ability(AbilityTerm.BRACE, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 100), new SelfTarget())));
    }
}
