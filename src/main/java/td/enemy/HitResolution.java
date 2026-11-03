package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.Delivery;
import td.stat.EnemyStat;
import td.stat.StatCurves;
import td.stat.StatView;
import td.util.RandomSource;

/**
 * The one formula a hit lands by: the dodge roll (aimed hits only), the crit roll (hits only), then, past any shield,
 * penetration, armor or magic resist, plating or insulation and damage taken. The crit is rounded where a shield
 * meets the hit, what gets past is rounded once at the end, and the cap at health comes after, on the mob.
 */
public final class HitResolution {

    private HitResolution() {
    }

    /** A hit from nobody in particular: no crit roll, no penetration. */
    public static Damage resolve(Damage incoming, StatView stats) {
        return resolve(incoming, AttackProfile.none(), stats, () -> 1.0);
    }

    /** What {@code incoming} lands for against {@code stats} with no shield in the way, uncapped. */
    public static Damage resolve(Damage incoming, AttackProfile attacker, StatView stats, RandomSource random) {
        return pastShields(raw(incoming, attacker, stats, random), attacker, stats);
    }

    /**
     * What {@code incoming} comes to as it meets the enemy, before any shield: its type kept, critical if it arrived
     * critical or the roll succeeds, a rolled crit's factor applied. The random source is only drawn from when the
     * crit chance is above zero, so attackers without crit leave the sequence untouched.
     */
    public static Damage raw(Damage incoming, AttackProfile attacker, StatView stats, RandomSource random) {
        float resilience = stats.value(EnemyStat.RESILIENCE);
        boolean rolledCrit = !incoming.critical() && attacker.delivery() == Delivery.HIT && resilience < 100f
                && rollsCrit(attacker, resilience, random);
        int amount = rolledCrit ? Math.round(incoming.amount() * critFactor(attacker, stats)) : incoming.amount();
        return new Damage(amount, incoming.type(), incoming.critical() || rolledCrit);
    }

    /** What {@code raw}, the part of a hit no shield held, lands for against {@code stats}, uncapped. */
    public static Damage pastShields(Damage raw, AttackProfile attacker, StatView stats) {
        DamageType type = raw.type();
        boolean critical = raw.critical();
        float amount = afterMitigation(raw.amount(), type, attacker, stats, critical);
        amount = afterFlatReduction(amount, type, attacker, stats, critical);
        amount = afterDamageTaken(amount, type, stats);
        if (attacker.delivery() == Delivery.PERIODIC) {
            amount *= stats.value(EnemyStat.PERIODIC_DAMAGE_TAKEN);
        }
        return new Damage(Math.round(amount), type, critical);
    }

    /**
     * {@link #raw} and {@link #pastShields} with the crit already decided, one step at a time, for a trace of the
     * hit: the same numbers, never a second roll.
     *
     * @param rolledCrit whether the crit roll succeeded; a hit that arrived critical is not rolled
     * @param absorbed   the units of the raw hit the shields held
     */
    public static HitSteps steps(Damage incoming, AttackProfile attacker, StatView stats, boolean rolledCrit,
                                 int absorbed) {
        boolean critical = incoming.critical() || rolledCrit;
        DamageType type = incoming.type();
        HitSteps steps = HitSteps.arriving(incoming, attacker.delivery());
        float amount = incoming.amount();
        if (rolledCrit) {
            float factor = critFactor(attacker, stats);
            amount = Math.round(amount * factor);
            steps = steps.then(HitStep.crit(factor, amount));
        }
        if (absorbed > 0) {
            amount -= absorbed;
            steps = steps.then(HitStep.shield(absorbed, amount));
        }
        float mitigation = stats.value(EnemyStat.mitigationFor(type));
        amount = afterMitigation(amount, type, attacker, stats, critical);
        steps = steps.then(HitStep.mitigation(mitigation, attacker.penetrate(type, mitigation, critical), amount));
        float flat = stats.value(EnemyStat.flatReductionFor(type));
        amount = afterFlatReduction(amount, type, attacker, stats, critical);
        steps = steps.then(HitStep.flatReduction(flat, attacker.penetrateFlatReduction(flat, critical), amount));
        float taken = stats.value(EnemyStat.damageTakenFor(type));
        amount = afterDamageTaken(amount, type, stats);
        steps = steps.then(HitStep.damageTaken(taken, amount));
        if (attacker.delivery() == Delivery.PERIODIC) {
            float periodic = stats.value(EnemyStat.PERIODIC_DAMAGE_TAKEN);
            amount *= periodic;
            steps = steps.then(HitStep.periodicTaken(periodic, amount));
        }
        return steps.resolvedTo(new Damage(Math.round(amount), type, critical));
    }

    private static float afterMitigation(float amount, DamageType type, AttackProfile attacker, StatView stats,
                                         boolean critical) {
        return amount * mitigationMultiplier(attacker.penetrate(type, stats.value(EnemyStat.mitigationFor(type)), critical));
    }

    private static float afterFlatReduction(float amount, DamageType type, AttackProfile attacker, StatView stats,
                                            boolean critical) {
        return Math.max(0f, amount - attacker.penetrateFlatReduction(stats.value(EnemyStat.flatReductionFor(type)), critical));
    }

    private static float afterDamageTaken(float amount, DamageType type, StatView stats) {
        return amount * stats.value(EnemyStat.damageTakenFor(type));
    }

    /**
     * Whether {@code stats}' agility dodges a hit from {@code attacker}, before anything else about the hit
     * is decided. Only an aimed hit can be dodged, and never a guaranteed crit. The random source is only
     * drawn from when the chance is above zero, so a target without agility leaves the sequence untouched.
     */
    public static boolean dodges(AttackProfile attacker, StatView stats, RandomSource random) {
        if (!attacker.aimed() || attacker.delivery() != Delivery.HIT || attacker.guaranteedCrit()) {
            return false;
        }
        float chance = dodgeChance(stats.value(EnemyStat.AGILITY) - attacker.accuracy());
        return chance > 0f && random.nextDouble() < chance;
    }

    /** {@code agility / (agility + 100)}: each point matters less than the one before, and it never reaches 1. */
    public static float dodgeChance(float agility) {
        return StatCurves.dodgeChance(agility);
    }

    /** Whether a hit from {@code attacker} would roll a critical against {@code stats}. */
    public static boolean rollsCrit(AttackProfile attacker, StatView stats, RandomSource random) {
        float resilience = stats.value(EnemyStat.RESILIENCE);
        return resilience < 100f && rollsCrit(attacker, resilience, random);
    }

    private static boolean rollsCrit(AttackProfile attacker, float resilience, RandomSource random) {
        if (attacker.guaranteedCrit()) {
            return true;
        }
        float chance = attacker.critChance() * (1f - resilience / 100f);
        return chance > 0f && random.nextDouble() < chance;
    }

    /**
     * What a crit multiplies damage by against {@code stats}: the attacker's bonus, shrunk by
     * resilience. {@code 1} against a crit-immune target.
     */
    public static float critFactor(AttackProfile attacker, StatView stats) {
        return 1f + (attacker.critMultiplier() - 1f) * (1f - stats.value(EnemyStat.RESILIENCE) / 100f);
    }

    /**
     * The share of a {@code type} hit that mitigation and damage taken remove, before plating or insulation and with
     * no penetration: {@code 0} for none, negative when the enemy takes extra.
     */
    public static float reductionAgainst(DamageType type, StatView stats) {
        return 1f - mitigationMultiplier(stats.value(EnemyStat.mitigationFor(type)))
                * stats.value(EnemyStat.damageTakenFor(type));
    }

    /** {@code 100 / (100 + armor)}: each point matters less than the one before. */
    public static float mitigationMultiplier(float armor) {
        return StatCurves.mitigationMultiplier(armor);
    }
}
