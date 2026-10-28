package td.enemy;

import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.util.TickRate;

import java.util.List;

/**
 * The Armored mob ({@code s}): armor and magic resist, plating, immune to crits, heavy enough to shrug off
 * some of a freeze, and at Boss rank it calls reinforcements. Those are a definition of their own, not the ladder's
 * boss, because a boss that reinforces with itself would be a spawn cycle. From Elite it lays mines along its path that
 * shield the first enemy to step on them.
 */
final class ArmoredEnemy {

    private ArmoredEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("armored", "Armored mob", 120, 2, 1.28f, BodyArchetype.SQUARE)
                        .withDescription("Heavy plates blunt physical hits; magic goes through.")
                        .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                        .withArmor(25f)
                        .withMagicResist(25f)
                        .withPlating(2f)
                        .withResilience(100f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.named("freeze", EffectResistTrait.resisting(EffectKind.FREEZE, 0.2f)))),
                        RankGrowth.of(14f))
                .thenAt(Rank.SOLDIER, e -> e
                        .withArmor(33f)
                        .withMagicResist(33f))
                .thenAt(Rank.VETERAN, e -> e
                        .withArmor(43f)
                        .withMagicResist(43f)
                        .withPlating(3f)
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("chill", EffectResistTrait.resisting(EffectKind.CHILL, 0.25f)))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(67f, 67f, 8f, 0f, 100f, 5f, 10f)
                        .withAdditionalAbilities(List.of(brace(150f), wardMine(120f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(100f, 100f, 10f, 0f, 100f, 5f, 20f)
                        .withAdditionalAbilities(List.of(brace(300f), wardMine(240f), IdentifiedAbility.anonymous(new Ability(AbilityTerm.REINFORCE,
                                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                                new SpawnEnemiesAction("armoredSpawn", AbilitySpawnShape.brood(3, 1f, 1f), false))))))
                .build());
        catalog.register(EnemyDefinition.of("armoredSpawn", "Armored Reinforcement", 150, 1, 1.28f, BodyArchetype.SQUARE)
                .withDescription("Called in by an Armored boss to hold the line.")
                .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                .withArmor(25f)
                .withMagicResist(25f)
                .withPlating(2f));
    }

    /** A mine left on the path for a while, that shields the first enemy to step on it. */
    private static IdentifiedAbility wardMine(float shieldPoints) {
        return IdentifiedAbility.named("wardMine", new Ability(AbilityTerm.WARD_MINE,
                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                LeaveZoneAction.wardMine(0.5f, Math.round(8 * TickRate.TICKS_PER_SECOND), shieldPoints)));
    }

    /** The physical shield it raises once, at half health. */
    private static IdentifiedAbility brace(float points) {
        return IdentifiedAbility.named("brace", new Ability(AbilityTerm.BRACE, new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 100), new SelfTarget())));
    }
}
