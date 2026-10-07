package td.tower.sonar;

import td.damage.AttackProfile;
import td.damage.DamageType;

import java.util.function.UnaryOperator;

/**
 * One beam hit as it will land, before it does: the base hit reshaped by each perk the Sonar
 * owns. Each {@code withX} is a copy.
 *
 * @param type         what kind of damage it deals
 * @param damageFactor a multiple of the Sonar's current damage, the beam's share included
 * @param attack       what the hit carries: crit, penetration
 * @param magicShare   a share of the hit that is added on as magic damage
 * @param farBonus     how much more it deals at the edge of the range, growing with distance
 */
public record SonarStrike(DamageType type, float damageFactor, AttackProfile attack, float magicShare,
                          float farBonus) {

    public static SonarStrike of(AttackProfile attack, float beamShare) {
        return new SonarStrike(DamageType.PHYSICAL, beamShare, attack, 0f, 0f);
    }

    public SonarStrike withType(DamageType type) {
        return new SonarStrike(type, this.damageFactor, this.attack, this.magicShare, this.farBonus);
    }

    public SonarStrike withAttack(UnaryOperator<AttackProfile> change) {
        return new SonarStrike(this.type, this.damageFactor, change.apply(this.attack), this.magicShare,
                this.farBonus);
    }

    public SonarStrike withMagicShare(float magicShare) {
        return new SonarStrike(this.type, this.damageFactor, this.attack, magicShare, this.farBonus);
    }

    /** The bonus at the edge of the range; a perk that gives more replaces one that gives less. */
    public SonarStrike withFarBonus(float farBonus) {
        return new SonarStrike(this.type, this.damageFactor, this.attack, this.magicShare,
                Math.max(this.farBonus, farBonus));
    }
}
