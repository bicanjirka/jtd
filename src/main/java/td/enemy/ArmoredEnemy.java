package td.enemy;

import td.effect.ShieldTemplate;
import td.stat.EnemyStat;
import td.util.TickRate;

import java.util.List;

/**
 * The Armored mob ({@code s}): less damage from every hit, immune to crits, heavy enough to shrug off some of a
 * freeze, and at Boss rank it calls reinforcements. Those are a definition of their own, not the ladder's boss,
 * because a boss that reinforces with itself would be a spawn cycle.
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
                        .withStat(EnemyStat.PHYSICAL_PLATING, 2f)
                        .withStat(EnemyStat.FREEZE_RESIST, 0.2f)
                        .withIdentifiedTraits(List.of(IdentifiedTrait.named("resist", ArmorTrait.both(25f)),
                                IdentifiedTrait.named("criticalImmune", new CriticalImmunityTrait()))),
                        RankGrowth.of(1.8f, 4))
                .thenAt(Rank.SOLDIER, e -> e
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("resist", ArmorTrait.both(33f)))))
                .thenAt(Rank.VETERAN, e -> e
                        .withStat(EnemyStat.CHILL_RESIST, 0.25f)
                        .withAdditionalTraits(List.of(IdentifiedTrait.named("resist", ArmorTrait.both(43f)))))
                .thenAt(Rank.ELITE, e -> e
                        .withDescription("Takes far less damage, and its plating has grown thick enough to blunt a flat "
                                + "chunk of every hit outright. Braces behind a physical shield once badly hurt. "
                                + "Immune to critical hits.")
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("flatResist", FlatResistTrait.physicalOnly(8)),
                                IdentifiedTrait.named("resist", ArmorTrait.both(67f))))
                        .withAdditionalAbilities(List.of(brace(150f))))
                .thenAt(Rank.BOSS, e -> e
                        .withDescription("Takes drastically less damage, and its plating blunts a large flat chunk of "
                                + "every hit outright. Braces behind a physical shield once badly hurt. Immune to "
                                + "critical hits. Calls three armored reinforcements every three seconds.")
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("flatResist", FlatResistTrait.physicalOnly(10)),
                                IdentifiedTrait.named("resist", ArmorTrait.both(100f))))
                        .withAdditionalAbilities(List.of(brace(300f), IdentifiedAbility.anonymous(new Ability(
                                new PeriodicTrigger(Math.round(3 * TickRate.TICKS_PER_SECOND)),
                                new SpawnEnemiesAction("sSpawn", AbilitySpawnShape.brood(3, 1f, 1f), false))))))
                .build());
        catalog.register(EnemyDefinition.of("sSpawn", "Armored Reinforcement", 200, 1, 1.28f, BodyArchetype.SQUARE)
                .withDescription("A reinforcement called by an Armored boss. Takes less damage.")
                .withMovement(new RotorMovement((float) Math.toRadians(5.0)))
                .withTraits(List.of(ArmorTrait.both(25f))));
    }

    /** The physical shield it raises once, at half health. */
    private static IdentifiedAbility brace(float points) {
        return IdentifiedAbility.named("brace", new Ability(new HealthThresholdTrigger(0.5f),
                new ApplyEffectAction(ShieldTemplate.physicalOnly(points, 100), new SelfTarget())));
    }
}
