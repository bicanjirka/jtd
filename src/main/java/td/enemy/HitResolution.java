package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.Delivery;
import td.stat.EnemyStat;
import td.stat.StatView;
import td.util.RandomSource;

/**
 * The one formula a hit lands by: the dodge roll (aimed hits only), the crit roll (hits only), then penetration,
 * armor or magic resist, plating or insulation and damage taken. Computed in float and rounded once; a shield and the cap at health come after, on the mob.
 */
public final class HitResolution {

    private HitResolution() {
    }

    /** A hit from nobody in particular: no crit roll, no penetration. */
    public static Damage resolve(Damage incoming, StatView stats) {
        return resolve(incoming, AttackProfile.none(), stats, () -> 1.0);
    }

    /**
     * What {@code incoming} lands for against {@code stats}, before any shield and uncapped; its type
     * is kept, and it is critical if it arrived critical or the roll succeeds. The random source is
     * only drawn from when the crit chance is above zero, so attackers without crit leave the sequence
     * untouched.
     */
    public static Damage resolve(Damage incoming, AttackProfile attacker, StatView stats, RandomSource random) {
        float resilience = stats.value(EnemyStat.RESILIENCE);
        boolean rolledCrit = !incoming.critical() && attacker.delivery() == Delivery.HIT && resilience < 100f
                && rollsCrit(attacker, resilience, random);
        boolean critical = incoming.critical() || rolledCrit;
        DamageType type = incoming.type();
        float amount = rolledCrit ? incoming.amount() * critFactor(attacker, stats) : incoming.amount();
        amount = afterMitigation(amount, type, attacker, stats, critical);
        amount = afterFlatReduction(amount, type, attacker, stats, critical);
        amount = afterDamageTaken(amount, type, stats);
        if (attacker.delivery() == Delivery.PERIODIC) {
            amount *= stats.value(EnemyStat.PERIODIC_DAMAGE_TAKEN);
        }
        return new Damage(Math.round(amount), type, critical);
    }

    /**
     * {@link #resolve} with the crit already decided, one step at a time, for a trace of the hit: the same
     * numbers, never a second roll.
     *
     * @param rolledCrit whether the crit roll succeeded; a hit that arrived critical is not rolled
     */
    public static HitSteps steps(Damage incoming, AttackProfile attacker, StatView stats, boolean rolledCrit) {
        boolean critical = incoming.critical() || rolledCrit;
        DamageType type = incoming.type();
        HitSteps steps = HitSteps.arriving(incoming, attacker.delivery());
        float amount = incoming.amount();
        if (rolledCrit) {
            float factor = critFactor(attacker, stats);
            amount *= factor;
            steps = steps.then(HitStep.crit(factor, amount));
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
        float effective = Math.max(0f, agility);
        return effective / (effective + 100f);
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
        return 100f / (100f + Math.max(0f, armor));
    }
}
