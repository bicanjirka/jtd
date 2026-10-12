package td.tower.sniper;

import td.damage.AttackProfile;
import td.damage.DamageType;

import java.util.function.UnaryOperator;

/**
 * One shot as it will land, before it does: the base shot reshaped by each perk the Sniper owns.
 * Each {@code withX} is a copy.
 *
 * @param type          what kind of damage it deals
 * @param damageFactor  a multiple of the Sniper's current damage
 * @param attack        what the hit carries: crit, penetration
 * @param attackSpeedBonus extra attack speed for the wait that follows
 * @param piercing      whether it hits every enemy on the line through the target
 */
public record SniperShot(DamageType type, float damageFactor, AttackProfile attack, float attackSpeedBonus,
                         boolean piercing) {

    /** A plain physical shot at the Sniper's current damage. */
    public static SniperShot of(AttackProfile attack) {
        return new SniperShot(DamageType.PHYSICAL, 1f, attack, 0f, false);
    }

    public SniperShot withType(DamageType type) {
        return new SniperShot(type, this.damageFactor, this.attack, this.attackSpeedBonus, this.piercing);
    }

    public SniperShot withDamageFactor(float damageFactor) {
        return new SniperShot(this.type, damageFactor, this.attack, this.attackSpeedBonus, this.piercing);
    }

    /** Multiplies the damage by {@code factor}. */
    public SniperShot scaledBy(float factor) {
        return new SniperShot(this.type, this.damageFactor * factor, this.attack, this.attackSpeedBonus, this.piercing);
    }

    public SniperShot withAttack(UnaryOperator<AttackProfile> change) {
        return new SniperShot(this.type, this.damageFactor, change.apply(this.attack), this.attackSpeedBonus,
                this.piercing);
    }

    /** Adds {@code bonus} to the crit chance, which stays a probability. */
    public SniperShot withCritChanceBonus(float bonus) {
        return this.withAttack(attack -> attack.withCritChance(Math.min(1f, attack.critChance() + bonus)));
    }

    /** Attack-speed bonuses add. */
    public SniperShot withAttackSpeedBonus(float bonus) {
        return new SniperShot(this.type, this.damageFactor, this.attack, this.attackSpeedBonus + bonus, this.piercing);
    }

    public SniperShot withPiercing() {
        return new SniperShot(this.type, this.damageFactor, this.attack, this.attackSpeedBonus, true);
    }
}
