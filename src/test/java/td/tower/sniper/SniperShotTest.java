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
    void aPlainShotIsPhysicalAtFullDamageWithNoExtraSpeedOrBonus() {
        SniperShot shot = plain();

        assertThat(shot.type()).isEqualTo(DamageType.PHYSICAL);
        assertThat(shot.damageFactor()).isEqualTo(1f);
        assertThat(shot.attackSpeedBonus()).isZero();
        assertThat(shot.bonusDamage()).isZero();
    }

    @Test
    void damageFactorsMultiply() {
        assertThat(plain().scaledBy(1.5f).scaledBy(2f).damageFactor()).isEqualTo(3f);
    }

    @Test
    void attackSpeedBonusesAdd() {
        float combined = plain().withAttackSpeedBonus(0.25f).withAttackSpeedBonus(0.5f).attackSpeedBonus();

        assertThat(combined).isCloseTo(0.75f, within(1e-6f));
    }

    @Test
    void bonusDamageIsAddedBeforeTheFactor() {
        SniperShot shot = plain().withBonusDamage(10f).withBonusDamage(30f).scaledBy(1.5f);

        assertThat(shot.amountOn(60f)).isEqualTo(150);
    }
}
