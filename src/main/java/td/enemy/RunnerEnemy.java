package td.enemy;

import td.effect.HealTemplate;
import td.effect.SprintTemplate;
import td.zone.Zone;

import java.util.List;

/**
 * The Runner ({@code runner}): faster the more it is hurt, more so with rank. From Veteran it is nimbler and throws
 * down ground once badly hurt, sooner from Elite, that speeds up the enemies who cross it and keeps slows off them
 * until the wave is over; from Elite it bolts once badly hurt and dodges more the more it is hurt; at Boss rank it calls a brood of spawnlings, heals
 * itself when left alone and no longer takes crits; the spawnlings are a definition of their own.
 */
final class RunnerEnemy {

    private RunnerEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("runner", "Runner", 90, 2, 1.28f, BodyArchetype.TRIANGLE)
                                .withDescription("Driven: the more it is hurt, the faster it runs.")
                                .withMovement(new RotorMovement((float) Math.toRadians(-5.0)))
                                .withAgility(5f)
                                .withIdentifiedTraits(List.of(hurtSpeed(1.4f))),
                        RankGrowth.stepped(150, 245, 760, 2500).paying(4, 6, 12, 17))
                .thenAt(Rank.VETERAN, e -> e
                        .withAgility(30f)
                        .withTenacity(5f)
                        .withAdditionalTraits(List.of(hurtSpeed(1.6f)))
                        .withAdditionalAbilities(List.of(hasteGround(0.25f, 1f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 10f, 0f, 0f, 20f, 10f, 0f, 10f)
                        .withAdditionalTraits(List.of(hurtSpeed(2f), IdentifiedTrait.named("skittish", new HurtAgilityTrait(30f))))
                        .withAdditionalAbilities(List.of(hasteGround(0.75f, 1f),
                                IdentifiedAbility.anonymous(new Ability(AbilityTerm.SECOND_WIND,
                                        new HealthThresholdTrigger(0.5f),
                                        new ApplyEffectAction(new SprintTemplate(40), new SelfTarget()))))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 15f, 0f, 0f, 40f, 33.33f, 100f, 20f)
                        .withAdditionalTraits(List.of(hurtSpeed(2.5f)))
                        .withMovement(new RotorMovement((float) Math.toRadians(-10.0)))
                        .withoutAbility("haste")
                        .withAdditionalAbilities(List.of(
                                IdentifiedAbility.anonymous(new Ability(AbilityTerm.SWARM, new HealthThresholdTrigger(0.5f),
                                        new SpawnEnemiesAction("frenzySpawnling", AbilitySpawnShape.brood(3, 1f, 1f), false))),
                                IdentifiedAbility.anonymous(new Ability(AbilityTerm.RECOVER, new TimeSinceLastHitTrigger(200),
                                        new ApplyEffectAction(new HealTemplate(4f, 200), new SelfTarget()))))))
                .build());
        catalog.register(EnemyDefinition.of("frenzySpawnling", "Frenzy Spawnling", 440, 1, 1.28f, BodyArchetype.TRIANGLE)
                .withDescription("Born of an enraged Runner boss; it tends its kin as it dies.")
                .withAgility(-20f)
                .withTraits(List.of(new HurtSpeedTrait(1.4f)))
                .withAbilities(List.of(new Ability(AbilityTerm.LAST_RITES, new OnDeathTrigger(),
                        new ApplyEffectAction(new HealTemplate(4f, 40), new RadiusTarget(150f))))));
    }

    private static IdentifiedTrait hurtSpeed(float maxMultiplier) {
        return IdentifiedTrait.named("hurtSpeed", new HurtSpeedTrait(maxMultiplier));
    }

    /** Ground left where it is hurt to the given share of its health, kept until the wave is over. */
    private static IdentifiedAbility hasteGround(float hurtFraction, float radiusCells) {
        return IdentifiedAbility.named("haste", new Ability(AbilityTerm.HASTE_FIELD,
                new HealthThresholdTrigger(hurtFraction),
                LeaveZoneAction.hasteGround(radiusCells, Zone.UNTIL_WAVE_END)));
    }
}
