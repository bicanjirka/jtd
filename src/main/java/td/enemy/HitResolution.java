package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.stat.EnemyStat;
import td.stat.StatView;
import td.util.RandomSource;

/**
 * The one formula a hit lands by: the crit roll, then penetration, armor or magic resist, plating,
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
        float resilienceKept = 1f - stats.value(EnemyStat.RESILIENCE) / 100f;
        float critChance = attacker.critChance() * stats.value(EnemyStat.CRIT_CHANCE_TAKEN) * resilienceKept;
        if (!critical && critChance > 0f && random.nextDouble() < critChance) {
            critical = true;
            amount *= 1f + (attacker.critMultiplier() - 1f) * resilienceKept;
        }
        float mitigation = attacker.penetrate(incoming.type(), stats.value(EnemyStat.mitigationFor(incoming.type())));
        amount *= mitigationMultiplier(mitigation);
        amount = Math.max(0f, amount - attacker.penetratePlating(stats.value(EnemyStat.platingFor(incoming.type()))));
        amount *= stats.value(EnemyStat.damageTakenFor(incoming.type()));
        return new Damage(Math.round(amount), incoming.type(), critical).cappedAt(health);
    }

    /**
     * The share of a {@code type} hit that mitigation and damage taken remove, before plating and
     * with no penetration: {@code 0} for none, negative when the enemy takes extra.
     */
    public static float reductionAgainst(DamageType type, StatView stats) {
        return 1f - mitigationMultiplier(stats.value(EnemyStat.mitigationFor(type)))
                * stats.value(EnemyStat.damageTakenFor(type));
    }

    /**
     * {@code 100 / (100 + armor)}: each point matters less than the one before. Negative armor
     * mirrors it, approaching double damage.
     */
    public static float mitigationMultiplier(float armor) {
        if (armor >= 0f) {
            return 100f / (100f + armor);
        }
        return 2f - 100f / (100f - armor);
    }
}
