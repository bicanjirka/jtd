package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;

import java.util.Set;

/** An enemy as a hit sees it: what lands, what it is already suffering, and whether it died. */
public interface HitReceiver {

    /**
     * Applies a hit from {@code attacker} and returns what actually landed, which may be less,
     * more (a crit) or nothing. Report the return value, not the argument.
     */
    Damage doDamage(Damage damage, AttackProfile attacker);

    /** A hit from an attacker with no crit or penetration. */
    default Damage doDamage(Damage damage) {
        return this.doDamage(damage, AttackProfile.none());
    }

    void applyEffect(Effect effect);

    /** Active effect kinds, in a stable order. */
    Set<EffectKind> activeEffectKinds();

    boolean isDead();

    /** Paid on death, and lost as score if it leaks. */
    int getBounty();
}
