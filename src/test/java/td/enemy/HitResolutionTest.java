package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageType;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatView;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HitResolutionTest {

    private static StatView stats(BaseStats base) {
        return base::value;
    }

    @Test
    void armorFollowsAHyperbolicCurve() {
        assertThat(HitResolution.mitigationMultiplier(0f)).isEqualTo(1f);
        assertThat(HitResolution.mitigationMultiplier(100f)).isEqualTo(0.5f);
        assertThat(HitResolution.mitigationMultiplier(300f)).isEqualTo(0.25f);
    }

    @Test
    void negativeArmorAmplifiesDamageTowardDouble() {
        assertThat(HitResolution.mitigationMultiplier(-100f)).isEqualTo(1.5f);
        assertThat(HitResolution.mitigationMultiplier(-10000f)).isCloseTo(2f, within(0.02f));
    }

    @Test
    void armorOnlyMitigatesPhysicalAndMagicResistOnlyMagic() {
        StatView armored = stats(BaseStats.defaults().with(EnemyStat.ARMOR, 100f));

        assertThat(HitResolution.resolve(Damage.physical(1000), armored, 10000)).isEqualTo(Damage.physical(500));
        assertThat(HitResolution.resolve(Damage.magic(1000), armored, 10000)).isEqualTo(Damage.magic(1000));
    }

    @Test
    void platingComesOffAfterArmorAndNeverBelowZero() {
        StatView plated = stats(BaseStats.defaults().with(EnemyStat.ARMOR, 100f).with(EnemyStat.PHYSICAL_PLATING, 300f));

        assertThat(HitResolution.resolve(Damage.physical(1000), plated, 10000)).isEqualTo(Damage.physical(200));
        assertThat(HitResolution.resolve(Damage.physical(500), plated, 10000)).isEqualTo(Damage.physical(0));
    }

    @Test
    void damageTakenMultipliesLastAndTheLandedHitIsCappedAtHealth() {
        StatView vulnerable = stats(BaseStats.defaults().with(EnemyStat.MAGIC_DAMAGE_TAKEN, 1.5f));

        assertThat(HitResolution.resolve(Damage.magic(100), vulnerable, 10000)).isEqualTo(Damage.magic(150));
        assertThat(HitResolution.resolve(Damage.magic(100), vulnerable, 40)).isEqualTo(Damage.magic(40));
    }

    @Test
    void aCriticalHitStaysCriticalThroughMitigation() {
        StatView armored = stats(BaseStats.defaults().with(EnemyStat.ARMOR, 100f));

        assertThat(HitResolution.resolve(new Damage(1000, DamageType.PHYSICAL, true), armored, 10000).critical())
                .isTrue();
    }
}
