package td.tower.sniper;

import td.damage.AttackProfile;
import td.damage.DamageType;

import java.util.function.UnaryOperator;

/**
 * One shot as it will land, before it does: the base shot reshaped by each mechanic the Sniper owns.
 * Each {@code withX} is a copy.
 *
 * @param type             what kind of damage it deals
 * @param damageFactor     a multiple of the Sniper's current damage
 * @param bonusDamage      damage added before the factor, in the units of a tower's damage stat
 * @param attack           what the hit carries: crit, penetration
 * @param attackSpeedBonus extra attack speed for the wait that follows
 */
public record SniperShot(DamageType type, float damageFactor, float bonusDamage, AttackProfile attack,
                         float attackSpeedBonus) {

    /** A plain physical shot at the Sniper's current damage. */
    public static SniperShot of(AttackProfile attack) {
        return new SniperShot(DamageType.PHYSICAL, 1f, 0f, attack, 0f);
    }

    public SniperShot withType(DamageType type) {
        return new SniperShot(type, this.damageFactor, this.bonusDamage, this.attack, this.attackSpeedBonus);
    }

    /** Multiplies the damage by {@code factor}. */
    public SniperShot scaledBy(float factor) {
        return new SniperShot(this.type, this.damageFactor * factor, this.bonusDamage, this.attack,
                this.attackSpeedBonus);
    }

    /** Bonus damage adds. */
    public SniperShot withBonusDamage(float damage) {
        return new SniperShot(this.type, this.damageFactor, this.bonusDamage + damage, this.attack,
                this.attackSpeedBonus);
    }

    public SniperShot withAttack(UnaryOperator<AttackProfile> change) {
        return new SniperShot(this.type, this.damageFactor, this.bonusDamage, change.apply(this.attack),
                this.attackSpeedBonus);
    }

    /** Attack-speed bonuses add. */
    public SniperShot withAttackSpeedBonus(float bonus) {
        return new SniperShot(this.type, this.damageFactor, this.bonusDamage, this.attack,
                this.attackSpeedBonus + bonus);
    }

    /** The same shot aimed at one enemy, which that enemy's agility may dodge. */
    public SniperShot aimed() {
        return this.withAttack(AttackProfile::asAimed);
    }

    /** What it deals on a Sniper whose damage stat is {@code damage}. */
    public int amountOn(float damage) {
        return Math.round((damage + this.bonusDamage) * this.damageFactor);
    }
}
