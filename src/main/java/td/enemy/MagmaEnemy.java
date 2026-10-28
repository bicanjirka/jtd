package td.enemy;

import td.effect.EffectKind;
import td.effect.SpellproofTemplate;
import td.effect.UnyieldingTemplate;
import td.util.TickRate;

import java.util.List;

/**
 * The Magma ({@code magma}): the fire-proof counterpart of the Yeti, slow and heavy. It shrugs off most burn and from
 * Veteran all of it, resists magic and almost never takes a crit. Once badly hurt it wards the enemies around it
 * against magic, and nearly dead it turns almost proof against magic and crits for a while. From Veteran it leaves pools
 * of molten rock behind it that mend whatever stands in them, until it is too hurt to keep up.
 */
final class MagmaEnemy {

    private static final float POOL_INTERVAL_SECONDS = 1.5f;
    private static final float POOL_LIFETIME_SECONDS = 5f;
    private static final float SPELL_WARD_SECONDS = 15f;
    private static final float LAST_STAND_SECONDS = 10f;
    private static final float SPELL_WARD_RADIUS = 120f;

    private MagmaEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("magma", "Magma", 190, 4, 0.8f, BodyArchetype.MAGMA)
                        .withDescription("A slow, fire-proof hulk of molten rock.")
                        .withMovement(new PulseMovement(0.12f, 40, 20))
                        .withMagicResist(30f)
                        .withAgility(10f)
                        .withResilience(70f)
                        .withSpirit(30f)
                        .withIdentifiedTraits(List.of(
                                IdentifiedTrait.named("burn", EffectResistTrait.resisting(EffectKind.BURN, 0.75f))))
                        .withIdentifiedAbilities(List.of(spellWard(15), lastStand())), RankGrowth.of(20f))
                .thenAt(Rank.SOLDIER, e -> e
                        .withAdditionalAbilities(List.of(spellWard(30))))
                .thenAt(Rank.VETERAN, e -> e
                        .withAdditionalTraits(List.of(
                                IdentifiedTrait.named("burn", EffectResistTrait.immuneTo(EffectKind.BURN))))
                        .withAdditionalAbilities(List.of(spellWard(50), pool(0.35f, 1f, 0.5f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 45f, 0f, 0f, 85f, 10f, 45f)
                        .withAdditionalAbilities(List.of(pool(0.6f, 1.25f, 0.5f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(15f, 60f, 0f, 0f, 95f, 10f, 60f)
                        .withAdditionalAbilities(List.of(pool(1f, 1.5f, 0.25f))))
                .build());
    }

    /** A pool that heals every enemy in it {@code pointsPerTick}, dropped where it stands until it is too hurt. */
    private static IdentifiedAbility pool(float pointsPerTick, float radiusCells, float stopsBelowHealth) {
        return IdentifiedAbility.named("pool", new Ability(AbilityTerm.HEALING_POOL,
                new PeriodicTrigger(Math.round(POOL_INTERVAL_SECONDS * TickRate.TICKS_PER_SECOND), stopsBelowHealth),
                LeaveZoneAction.healingGround(radiusCells, Math.round(POOL_LIFETIME_SECONDS * TickRate.TICKS_PER_SECOND),
                        pointsPerTick)));
    }

    /** Once badly hurt: magic resist for the other enemies near it. */
    private static IdentifiedAbility spellWard(int stacks) {
        return IdentifiedAbility.named("spellWard", new Ability(AbilityTerm.SPELL_WARD, new HealthThresholdTrigger(0.65f),
                new ApplyEffectAction(
                        new SpellproofTemplate(stacks, Math.round(SPELL_WARD_SECONDS * TickRate.TICKS_PER_SECOND)),
                        new RadiusTarget(SPELL_WARD_RADIUS))));
    }

    /** Once nearly dead: far more resilience, spirit and magic resist. */
    private static IdentifiedAbility lastStand() {
        return IdentifiedAbility.named("lastStand", new Ability(AbilityTerm.LAST_STAND, new HealthThresholdTrigger(0.05f),
                new ApplyEffectAction(
                        new UnyieldingTemplate(Math.round(LAST_STAND_SECONDS * TickRate.TICKS_PER_SECOND)),
                        new SelfTarget())));
    }
}
