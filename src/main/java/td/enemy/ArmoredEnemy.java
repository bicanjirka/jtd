package td.enemy;

import td.effect.EffectKind;
import td.effect.ShieldTemplate;
import td.util.TickRate;

import java.util.List;

/**
 * The Armored mob ({@code s}): armor and magic resist, plating, immune to crits, heavy enough to shrug off
 * some of a freeze, and at Boss rank it calls reinforcements. Those are a definition of their own, not the ladder's
 * boss, because a boss that reinforces with itself would be a spawn cycle.
 */
final class ArmoredEnemy {

    private ArmoredEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("s", "Armored mob", 160, 4, 1.28f, BodyArchetype.SQUARE)
                        .withDescription("Takes less damage, and its plates blunt a little of every physical hit. "
                                + "Immune to critical hits; too heavy to stay frozen as long.")
                        .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                        .withArmor(25f)
                        .withMagicResist(25f)
                        .withPlating(2f)
                        .withResilience(100f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.named("freeze", EffectResistTrait.resisting(EffectKind.FREEZE, 0.2f)))))
                .thenAt(Rank.SOLDIER, e -> e.withHealthAndPrice(288, 8)
                        .withArmor(33f)
                        .withMagicResist(33f))
                .thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(518, 12)
                        .withArmor(43f)
                        .withMagicResist(43f)
                        .withPlating(3f)
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("chill", EffectResistTrait.resisting(EffectKind.CHILL, 0.25f)))))
                .thenAt(Rank.ELITE, e -> e.withHealthAndPrice(933, 24)
                        .withDescription("Takes far less damage, and its plating has grown thick enough to blunt a flat "
                                + "chunk of every physical hit outright. Braces behind a physical shield once badly "
                                + "hurt. Immune to critical hits.")
                        .withStats(67f, 67f, 8f, 0f, 100f, 5f, 10f)
                        .withAdditionalAbilities(List.of(brace(150f))))
                .thenAt(Rank.BOSS, e -> e.withHealthAndPrice(1680, 44)
                        .withDescription("Takes drastically less damage, and its plating blunts a large flat chunk of "
                                + "every physical hit outright. Braces behind a physical shield once badly hurt. Immune "
                                + "to critical hits. Calls three armored reinforcements every three seconds.")
                        .withStats(100f, 100f, 10f, 0f, 100f, 5f, 20f)
                        .withAdditionalAbilities(List.of(brace(300f), IdentifiedAbility.anonymous(new Ability(
                                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                                new SpawnEnemiesAction("sSpawn", AbilitySpawnShape.brood(3, 1f, 1f), false))))))
                .build());
        catalog.register(EnemyDefinition.of("sSpawn", "Armored Reinforcement", 200, 1, 1.28f, BodyArchetype.SQUARE)
                .withDescription("A reinforcement called by an Armored boss. Takes less damage.")
                .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                .withArmor(25f)
                .withMagicResist(25f)
                .withPlating(2f));
    }

    /** The physical shield it raises once, at half health. */
    private static IdentifiedAbility brace(float points) {
        return IdentifiedAbility.named("brace", new Ability(new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 100), new SelfTarget())));
    }
}
