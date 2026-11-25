package td.tower.proc;

import td.damage.AttackProfile;
import td.stat.StatModifier;
import td.stat.StatModifiers;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerModifiers;
import td.tower.TowerStat;

import java.util.ArrayList;
import java.util.List;

/**
 * What bonuses add to one hit: damage as a fraction, crit chance and crit damage, attack speed for
 * the wait after it, accuracy in agility points, and range in cells for picking and reaching the attack's target.
 * {@link #none()} is the identity of {@link #plus}.
 */
public record HitAdjustment(float damage, float critChance, float critMultiplier, float attackSpeed, float accuracy,
                            float range) {

    private static final HitAdjustment NONE = new HitAdjustment(0f, 0f, 0f, 0f, 0f, 0f);

    public static HitAdjustment none() {
        return NONE;
    }

    public static HitAdjustment damage(float fraction) {
        return new HitAdjustment(fraction, 0f, 0f, 0f, 0f, 0f);
    }

    public static HitAdjustment critChance(float fraction) {
        return new HitAdjustment(0f, fraction, 0f, 0f, 0f, 0f);
    }

    public static HitAdjustment critMultiplier(float bonus) {
        return new HitAdjustment(0f, 0f, bonus, 0f, 0f, 0f);
    }

    public static HitAdjustment attackSpeed(float fraction) {
        return new HitAdjustment(0f, 0f, 0f, fraction, 0f, 0f);
    }

    public static HitAdjustment accuracy(float points) {
        return new HitAdjustment(0f, 0f, 0f, 0f, points, 0f);
    }

    /** Range in cells, which only a bonus that stands with the tower's focus may add ({@link HitBonus#standing}). */
    public static HitAdjustment range(float cells) {
        return new HitAdjustment(0f, 0f, 0f, 0f, 0f, cells);
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
                this.accuracy + other.accuracy, this.range + other.range);
    }

    public HitAdjustment times(float factor) {
        return new HitAdjustment(this.damage * factor, this.critChance * factor, this.critMultiplier * factor,
                this.attackSpeed * factor, this.accuracy * factor, this.range * factor);
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

    /** The same change made to the tower's stats, for a bonus known before any target: damage as a factor on it. */
    public StatModifiers<TowerStat> asModifiers() {
        StatModifiers<TowerStat> changes = StatModifiers.none();
        if (this.damage != 0f) {
            changes = changes.plus(TowerModifiers.damageTimes(this.damageFactor()));
        }
        if (this.critChance != 0f) {
            changes = changes.plus(TowerModifiers.critChance(this.critChance));
        }
        if (this.critMultiplier != 0f) {
            changes = changes.plus(TowerModifiers.critMultiplier(this.critMultiplier));
        }
        if (this.attackSpeed != 0f) {
            changes = changes.plus(TowerModifiers.attackSpeed(this.attackSpeed));
        }
        if (this.accuracy != 0f) {
            changes = changes.plus(TowerModifiers.accuracy(this.accuracy));
        }
        if (this.range != 0f) {
            changes = changes.plus(TowerModifiers.range(this.range));
        }
        return changes;
    }

    /** "Damage +40%", "Crit chance +50%": each non-zero part, its stat named, joined with ", ". */
    public TermText describe() {
        List<TermText> parts = new ArrayList<>();
        part(parts, TowerStat.DAMAGE, this.damage);
        part(parts, TowerStat.CRIT_CHANCE, this.critChance);
        part(parts, TowerStat.CRIT_MULTIPLIER, this.critMultiplier);
        part(parts, TowerStat.ATTACK_SPEED, this.attackSpeed);
        if (this.accuracy != 0f) {
            parts.add(TowerStat.ACCURACY.changeText(StatModifier.flat(this.accuracy)));
        }
        if (this.range != 0f) {
            parts.add(TowerStat.RANGE.changeText(StatModifier.flat(this.range)));
        }
        return TermText.join(", ", parts);
    }

    private static void part(List<TermText> parts, TowerStat stat, float fraction) {
        if (fraction != 0f) {
            parts.add(TermText.of(stat).plus(" " + TermNumbers.signedPercent(fraction)));
        }
    }
}
