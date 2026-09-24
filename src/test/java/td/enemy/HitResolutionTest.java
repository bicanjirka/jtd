package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
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
    @Test
    void fullResilienceNeverLetsACritLand() {
        StatView resilient = stats(BaseStats.defaults().with(EnemyStat.RESILIENCE, 100f));

        Damage landed = HitResolution.resolve(Damage.physical(1000), AttackProfile.critChance(1f), resilient, 10000,
                () -> 0.0);

        assertThat(landed).isEqualTo(Damage.physical(1000));
    }

    @Test
    void halfResilienceHalvesBothTheCritChanceAndTheCritBonus() {
        StatView halfResilient = stats(BaseStats.defaults().with(EnemyStat.RESILIENCE, 50f));
        AttackProfile attacker = AttackProfile.critChance(0.8f).withCritMultiplier(3f);

        Damage justInside = HitResolution.resolve(Damage.physical(1000), attacker, halfResilient, 10000, () -> 0.39);
        Damage justOutside = HitResolution.resolve(Damage.physical(1000), attacker, halfResilient, 10000, () -> 0.41);

        assertThat(justInside).isEqualTo(new Damage(2000, DamageType.PHYSICAL, true));
        assertThat(justOutside).isEqualTo(Damage.physical(1000));
    }

    @Test
    void anAttackerWithoutCritChanceNeverDrawsFromTheRandomSource() {
        StatView plain = stats(BaseStats.defaults());

        HitResolution.resolve(Damage.physical(1000), AttackProfile.none(), plain, 10000, () -> {
            throw new IllegalStateException("drew a random number");
        });
    }

    @Test
    void penetrationLowersArmorButNeverBelowZero() {
        StatView armored = stats(BaseStats.defaults().with(EnemyStat.ARMOR, 100f));

        Damage halfPierced = HitResolution.resolve(Damage.physical(1000),
                AttackProfile.none().withArmorPenetration(0.5f, 0f), armored, 10000, () -> 1.0);
        Damage overPierced = HitResolution.resolve(Damage.physical(1000),
                AttackProfile.none().withArmorPenetration(0.5f, 500f), armored, 10000, () -> 1.0);

        assertThat(halfPierced.amount()).isEqualTo(Math.round(1000 * 100f / 150f));
        assertThat(overPierced).isEqualTo(Damage.physical(1000));
    }

    @Test
    void penetrationLeavesNegativeArmorAlone() {
        StatView shredded = stats(BaseStats.defaults().with(EnemyStat.ARMOR, -100f));

        Damage landed = HitResolution.resolve(Damage.physical(1000), AttackProfile.none().withArmorPenetration(1f, 50f),
                shredded, 10000, () -> 1.0);

        assertThat(landed).isEqualTo(Damage.physical(1500));
    }
}
