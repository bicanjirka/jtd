package td.enemy;

import td.damage.Damage;
import td.stat.EnemyStat;
import td.stat.StatView;

/**
 * The one formula a hit lands by: armor or magic resist, then plating, then damage taken, then the
 * cap at remaining health. Computed in float and rounded once.
 */
public final class HitResolution {

    private HitResolution() {
    }

    /** What {@code incoming} lands for against {@code stats}; its type and critical flag are kept. */
    public static Damage resolve(Damage incoming, StatView stats, int health) {
        float amount = incoming.amount() * mitigationMultiplier(stats.value(EnemyStat.mitigationFor(incoming.type())));
        amount = Math.max(0f, amount - stats.value(EnemyStat.platingFor(incoming.type())));
        amount *= stats.value(EnemyStat.damageTakenFor(incoming.type()));
        return new Damage(Math.round(amount), incoming.type(), incoming.critical()).cappedAt(health);
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
