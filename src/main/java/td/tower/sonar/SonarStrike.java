package td.tower.sonar;

import td.damage.AttackProfile;
import td.damage.DamageType;


/**
 * One beam hit as it will land, before it does: the base hit reshaped by each mechanic the Sonar
 * owns. Each {@code withX} is a copy.
 *
 * @param type         what kind of damage it deals
 * @param damageFactor a multiple of the Sonar's current damage, the beam's share included
 * @param attack       what the hit carries: crit, penetration
 * @param magicShare   a share of the hit that is added on as magic damage
 */
public record SonarStrike(DamageType type, float damageFactor, AttackProfile attack, float magicShare) {

    public static SonarStrike of(AttackProfile attack, float beamShare) {
        return new SonarStrike(DamageType.PHYSICAL, beamShare, attack, 0f);
    }

    public SonarStrike withType(DamageType type) {
        return new SonarStrike(type, this.damageFactor, this.attack, this.magicShare);
    }

    public SonarStrike withMagicShare(float magicShare) {
        return new SonarStrike(this.type, this.damageFactor, this.attack, magicShare);
    }
}
