package td.tower.sniper;

import td.damage.AttackProfile;
import td.damage.DamageType;

import java.util.function.UnaryOperator;

/**
 * One shot as it will land, before it does: the base shot reshaped by each mechanic the Sniper owns.
 * Each {@code withX} is a copy.
 *
 * @param type          what kind of damage it deals
 * @param damageFactor  a multiple of the Sniper's current damage
 * @param attack        what the hit carries: crit, penetration
 * @param attackSpeedBonus extra attack speed for the wait that follows
 * @param pierce        how it runs through the line past the target, when it does
 */
public record SniperShot(DamageType type, float damageFactor, AttackProfile attack, float attackSpeedBonus,
                         PierceRule pierce) {

    /** A plain physical shot at the Sniper's current damage. */
    public static SniperShot of(AttackProfile attack) {
        return new SniperShot(DamageType.PHYSICAL, 1f, attack, 0f, PierceRule.none());
    }

    public SniperShot withType(DamageType type) {
        return new SniperShot(type, this.damageFactor, this.attack, this.attackSpeedBonus, this.pierce);
    }

    public SniperShot withDamageFactor(float damageFactor) {
        return new SniperShot(this.type, damageFactor, this.attack, this.attackSpeedBonus, this.pierce);
    }

    /** Multiplies the damage by {@code factor}. */
    public SniperShot scaledBy(float factor) {
        return new SniperShot(this.type, this.damageFactor * factor, this.attack, this.attackSpeedBonus, this.pierce);
    }

    public SniperShot withAttack(UnaryOperator<AttackProfile> change) {
        return new SniperShot(this.type, this.damageFactor, change.apply(this.attack), this.attackSpeedBonus,
                this.pierce);
    }

    /** Adds {@code bonus} to the crit chance, which stays a probability. */
    public SniperShot withCritChanceBonus(float bonus) {
        return this.withAttack(attack -> attack.withCritChance(Math.min(1f, attack.critChance() + bonus)));
    }

    /** Attack-speed bonuses add. */
    public SniperShot withAttackSpeedBonus(float bonus) {
        return new SniperShot(this.type, this.damageFactor, this.attack, this.attackSpeedBonus + bonus, this.pierce);
    }

    /** The same shot aimed at one enemy, which that enemy's agility may dodge. */
    public SniperShot aimed() {
        return this.withAttack(AttackProfile::asAimed);
    }

    public SniperShot withPiercing(PierceRule pierce) {
        return new SniperShot(this.type, this.damageFactor, this.attack, this.attackSpeedBonus, pierce);
    }

    /** Whether it hits every enemy on the line through the target. */
    public boolean piercing() {
        return this.pierce.isActive();
    }
}
