package td.enemy;

import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.effect.Effect;
import td.effect.EffectKind;

import java.util.List;
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

    /**
     * The share of a {@code type} hit this enemy's armor or magic resist and damage taken remove
     * right now, for damage that scales with how well protected the target is.
     */
    float reductionAgainst(DamageType type);

    /**
     * What a crit from {@code attacker} would multiply damage by against this enemy right now,
     * {@code 1} if it is crit-immune. Lets a hit's crit scale the damage over time it starts.
     */
    float critFactorFor(AttackProfile attacker);

    /** Takes {@code fraction} of the shield this enemy has now; none if it has no shield. */
    void breakShield(float fraction);

    /** Whether an effect of {@code kind} is active. */
    boolean hasEffect(EffectKind kind);

    /** Stacks of the active {@code kind}; {@code 0} when inactive or the kind does not stack. */
    int effectStacks(EffectKind kind);

    /** All the damage that has landed on it so far, hits and periodic damage alike, in units. */
    long damageTaken();

    /** Every effect active on it now, in kind order; a dead enemy keeps the ones it died under. */
    List<Effect> activeEffects();

    /** Whether an effect that {@link EffectKind#stopsEnemy() stops it} is active. */
    default boolean isStopped() {
        for (EffectKind kind : EffectKind.stopping()) {
            if (this.hasEffect(kind)) {
                return true;
            }
        }
        return false;
    }

    /** Whether its abilities are kept from firing: it is stopped, or Silenced. */
    default boolean isSilenced() {
        return this.isStopped() || this.hasEffect(EffectKind.SILENCED);
    }

    /** Removes the shield and the heal it has, if any. */
    void dispelRestoratives();

    /** The share of a {@code type} hit its shield takes right now, from {@code 0} to {@code 0.9}. */
    float shieldingFor(DamageType type);

    /** Whether repeated freezing has made its next freeze last less than a full one. */
    boolean freezeDiminished();

    /** Active effect kinds, in a stable order. */
    Set<EffectKind> activeEffectKinds();

    boolean isDead();

    /** Paid on death, and lost as score if it leaks. */
    int getBounty();
}
