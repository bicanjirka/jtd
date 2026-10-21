package td.tower.proc;

import td.damage.AttackProfile;
import td.term.TermNumbers;

import java.util.ArrayList;
import java.util.List;

/**
 * What bonuses add to one hit: damage as a fraction, crit chance and crit damage, attack speed for
 * the wait after it, and accuracy in agility points. {@link #none()} is the identity of {@link #plus}.
 */
public record HitAdjustment(float damage, float critChance, float critMultiplier, float attackSpeed, float accuracy) {

    private static final HitAdjustment NONE = new HitAdjustment(0f, 0f, 0f, 0f, 0f);

    public static HitAdjustment none() {
        return NONE;
    }

    public static HitAdjustment damage(float fraction) {
        return new HitAdjustment(fraction, 0f, 0f, 0f, 0f);
    }

    public static HitAdjustment critChance(float fraction) {
        return new HitAdjustment(0f, fraction, 0f, 0f, 0f);
    }

    public static HitAdjustment critMultiplier(float bonus) {
        return new HitAdjustment(0f, 0f, bonus, 0f, 0f);
    }

    public static HitAdjustment attackSpeed(float fraction) {
        return new HitAdjustment(0f, 0f, 0f, fraction, 0f);
    }

    public static HitAdjustment accuracy(float points) {
        return new HitAdjustment(0f, 0f, 0f, 0f, points);
    }

    public HitAdjustment plus(HitAdjustment other) {
        if (other == NONE) {
            return this;
        }
        if (this == NONE) {
            return other;
        }
        return new HitAdjustment(this.damage + other.damage, this.critChance + other.critChance,
                this.critMultiplier + other.critMultiplier, this.attackSpeed + other.attackSpeed,
                this.accuracy + other.accuracy);
    }

    public HitAdjustment times(float factor) {
        return new HitAdjustment(this.damage * factor, this.critChance * factor, this.critMultiplier * factor,
                this.attackSpeed * factor, this.accuracy * factor);
    }

    /** The factor on the hit's damage: 1 + damage. */
    public float damageFactor() {
        return 1f + this.damage;
    }

    /** {@code attack} with this crit chance (kept within [0, 1]), crit damage and accuracy added. */
    public AttackProfile applyTo(AttackProfile attack) {
        if (this.critChance == 0f && this.critMultiplier == 0f && this.accuracy == 0f) {
            return attack;
        }
        AttackProfile adjusted = attack;
        if (this.critChance != 0f) {
            adjusted = adjusted.withCritChance(Math.clamp(adjusted.critChance() + this.critChance, 0f, 1f));
        }
        if (this.critMultiplier != 0f) {
            adjusted = adjusted.withCritDamageBonus(this.critMultiplier);
        }
        if (this.accuracy != 0f) {
            adjusted = adjusted.withAccuracy(adjusted.accuracy() + this.accuracy);
        }
        return adjusted;
    }

    /** "+40% damage", "+50% crit chance": the non-zero parts joined with ", ". */
    public String describe() {
        List<String> parts = new ArrayList<>();
        part(parts, "damage", this.damage);
        part(parts, "crit chance", this.critChance);
        part(parts, "crit damage", this.critMultiplier);
        part(parts, "attack speed", this.attackSpeed);
        if (this.accuracy != 0f) {
            parts.add(TermNumbers.signed(Math.round(this.accuracy)) + " accuracy");
        }
        return String.join(", ", parts);
    }

    private static void part(List<String> parts, String name, float fraction) {
        if (fraction != 0f) {
            parts.add(TermNumbers.signedPercent(fraction) + " " + name);
        }
    }
}
