package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.damage.Delivery;
import td.stat.EnemyStat;
import td.stat.StatView;
import td.util.RandomSource;

/**
 * The one formula a hit lands by: the crit roll (hits only), then penetration, armor or magic resist, plating,
 * damage taken, and the cap at remaining health. Computed in float and rounded once.
 */
public final class HitResolution {

    private HitResolution() {
    }

    /** A hit from nobody in particular: no crit roll, no penetration. */
    public static Damage resolve(Damage incoming, StatView stats, int health) {
        return resolve(incoming, AttackProfile.none(), stats, health, () -> 1.0);
    }

    /**
     * What {@code incoming} lands for against {@code stats}; its type is kept, and it is critical if
     * it arrived critical or the roll succeeds. The random source is only drawn from when the crit
     * chance is above zero, so attackers without crit leave the sequence untouched.
     */
    public static Damage resolve(Damage incoming, AttackProfile attacker, StatView stats, int health,
            RandomSource random) {
        float amount = incoming.amount();
        boolean critical = incoming.critical();
        float resilience = stats.value(EnemyStat.RESILIENCE);
        if (!critical && attacker.delivery() == Delivery.HIT && resilience < 100f
                && rollsCrit(attacker, stats, resilience, random)) {
            critical = true;
            amount *= critFactor(attacker, stats);
        }
        float mitigation = attacker.penetrate(incoming.type(), stats.value(EnemyStat.mitigationFor(incoming.type())));
        amount *= mitigationMultiplier(mitigation);
        amount = Math.max(0f, amount - attacker.penetratePlating(stats.value(EnemyStat.platingFor(incoming.type()))));
        amount *= stats.value(EnemyStat.damageTakenFor(incoming.type()));
        return new Damage(Math.round(amount), incoming.type(), critical).cappedAt(health);
    }

    private static boolean rollsCrit(AttackProfile attacker, StatView stats, float resilience, RandomSource random) {
        if (attacker.guaranteedCrit()) {
            return true;
        }
        // Negative resilience never makes a crit likelier, only harder-hitting.
        float chance = attacker.critChance() * stats.value(EnemyStat.CRIT_CHANCE_TAKEN)
                * Math.min(1f, 1f - resilience / 100f);
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
     * The share of a {@code type} hit that mitigation and damage taken remove, before plating and
     * with no penetration: {@code 0} for none, negative when the enemy takes extra.
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
