package td.enemy;

import td.effect.EffectKind;
import td.effect.SpellproofTemplate;
import td.effect.UnyieldingTemplate;
import td.util.TickRate;

import java.util.List;

/**
 * The Magma ({@code magma}): the fire-proof counterpart of the Yeti, slow and heavy. It cannot burn, resists magic
 * heavily and almost never takes a crit, but whatever holds it back holds it far longer, less so with rank. From Soldier, once badly hurt, it wards the enemies around it against magic; from Veteran, nearly dead, it
 * turns almost proof against hits, crits and debuffs for a while, and it leaves pools of molten rock behind it that mend
 * whatever stands in them, until it is too hurt to keep up. At Boss it keeps its ward to itself and its pools run
 * hotter and longer.
 */
final class MagmaEnemy {

    private static final float POOL_INTERVAL_SECONDS = 1.5f;
    private static final float POOL_LIFETIME_SECONDS = 5f;
    private static final float LAST_STAND_SECONDS = 10f;
    private static final float SPELL_WARD_RADIUS = 120f;

    private MagmaEnemy() {
    }

    static void registerIn(EnemyCatalog catalog) {
        catalog.register(RankedEnemy
                .startingAt(EnemyDefinition.of("magma", "Magma", 163, 3, 0.73f, BodyArchetype.MAGMA)
                                .withDescription("A slow, fire-proof hulk of molten rock.")
                                .withMovement(new PulseMovement(0.3f, 40, 20))
                                .withMagicResist(50f)
                                .withAgility(10f)
                                .withResilience(70f)
                                .withTenacity(-45f)
                                .withSpirit(10f)
                                .withTraits(List.of(new ImmunityTrait(EffectKind.BURN))),
                        RankGrowth.stepped(360, 842, 2012, 2456).paying(5, 8, 17, 28))
                .thenAt(Rank.SOLDIER, e -> e
                        .withMagicResist(75f)
                        .withTenacity(0f)
                        .withAdditionalAbilities(List.of(spellWard(30))))
                .thenAt(Rank.VETERAN, e -> e
                        .withMagicResist(100f)
                        .withAdditionalAbilities(List.of(spellWard(50), lastStand(), pool(1f, 0.75f, 0.5f))))
                .thenAt(Rank.ELITE, e -> e
                        .withStats(10f, 135f, 0f, 0f, 10f, 0f, 85f, 15f)
                        .withAdditionalAbilities(List.of(pool(1.66f, 0.85f, 0.5f))))
                .thenAt(Rank.BOSS, e -> e
                        .withStats(25f, 200f, 4f, 2f, 10f, 0f, 95f, 20f)
                        .withoutAbility("spellWard")
                        .withAdditionalAbilities(List.of(pool(4f, 1f, 0.25f))))
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
                        new SpellproofTemplate(stacks),
                        new RadiusTarget(SPELL_WARD_RADIUS))));
    }

    /** Once nearly dead: far harder to hurt, to crit or to hold back for a while. */
    private static IdentifiedAbility lastStand() {
        return IdentifiedAbility.named("lastStand", new Ability(AbilityTerm.LAST_STAND, new HealthThresholdTrigger(0.1f),
                new ApplyEffectAction(
                        new UnyieldingTemplate(Math.round(LAST_STAND_SECONDS * TickRate.TICKS_PER_SECOND)),
                        new SelfTarget())));
    }
}
