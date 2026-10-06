package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.DamageType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class SniperShotTest {

    private static SniperShot plain() {
        return SniperShot.of(AttackProfile.critChance(0.3f));
    }

    @Test
    void aPlainShotIsPhysicalAtFullDamageWithNoExtraSpeedAndHitsOne() {
        SniperShot shot = plain();

        assertThat(shot.type()).isEqualTo(DamageType.PHYSICAL);
        assertThat(shot.damageFactor()).isEqualTo(1f);
        assertThat(shot.fireRateBonus()).isZero();
        assertThat(shot.piercing()).isFalse();
    }

    @Test
    void damageFactorsMultiply() {
        assertThat(plain().scaledBy(1.5f).scaledBy(2f).damageFactor()).isEqualTo(3f);
    }

    @Test
    void aCritChanceBonusAddsAndStaysAProbability() {
        assertThat(plain().withCritChanceBonus(0.2f).attack().critChance()).isCloseTo(0.5f, within(1e-6f));
        assertThat(plain().withCritChanceBonus(2f).attack().critChance()).isEqualTo(1f);
    }

    @Test
    void fireRateBonusesEachCutWhatIsLeftOfTheCooldown() {
        float combined = plain().withFireRateBonus(0.25f).withFireRateBonus(0.5f).fireRateBonus();

        assertThat(combined).isCloseTo(1f - 0.75f * 0.5f, within(1e-6f));
    }
}
