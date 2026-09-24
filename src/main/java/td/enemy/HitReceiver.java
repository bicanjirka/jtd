package td.enemy;

import td.damage.Damage;
import td.effect.Effect;
import td.effect.EffectKind;

import java.util.Set;

/** An enemy as a hit sees it: what lands, what it is already suffering, and whether it died. */
public interface HitReceiver {

    /**
     * Applies a hit and returns what actually landed, which may be less or nothing. Report the
     * return value, not the argument.
     */
    Damage doDamage(Damage damage);

    void applyEffect(Effect effect);

    /** Active effect kinds, in a stable order. */
    Set<EffectKind> activeEffectKinds();

    boolean isDead();

    /** Paid on death, and lost as score if it leaks. */
    int getBounty();
}
