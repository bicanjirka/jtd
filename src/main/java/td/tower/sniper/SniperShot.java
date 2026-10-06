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
 * @param fireRateBonus extra fire rate for the wait that follows, a fraction of the cooldown it cuts
 * @param piercing      whether it hits every enemy on the line through the target
 */
public record SniperShot(DamageType type, float damageFactor, AttackProfile attack, float fireRateBonus,
                         boolean piercing) {

    /** A plain physical shot at the Sniper's current damage. */
    public static SniperShot of(AttackProfile attack) {
        return new SniperShot(DamageType.PHYSICAL, 1f, attack, 0f, false);
    }

    public SniperShot withType(DamageType type) {
        return new SniperShot(type, this.damageFactor, this.attack, this.fireRateBonus, this.piercing);
    }

    /** Multiplies the damage by {@code factor}. */
    public SniperShot scaledBy(float factor) {
        return new SniperShot(this.type, this.damageFactor * factor, this.attack, this.fireRateBonus, this.piercing);
    }

    public SniperShot withAttack(UnaryOperator<AttackProfile> change) {
        return new SniperShot(this.type, this.damageFactor, change.apply(this.attack), this.fireRateBonus,
                this.piercing);
    }

    /** Adds {@code bonus} to the crit chance, which stays a probability. */
    public SniperShot withCritChanceBonus(float bonus) {
        return this.withAttack(attack -> attack.withCritChance(Math.min(1f, attack.critChance() + bonus)));
    }

    /** Fire-rate bonuses each cut what is left of the cooldown, so they multiply. */
    public SniperShot withFireRateBonus(float bonus) {
        float combined = this.fireRateBonus + bonus - this.fireRateBonus * bonus;
        return new SniperShot(this.type, this.damageFactor, this.attack, combined, this.piercing);
    }

    public SniperShot withPiercing() {
        return new SniperShot(this.type, this.damageFactor, this.attack, this.fireRateBonus, true);
    }
}
