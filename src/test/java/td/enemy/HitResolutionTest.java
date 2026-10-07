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
    void armorBelowZeroMitigatesNothingAndNeverAmplifies() {
        assertThat(HitResolution.mitigationMultiplier(-100f)).isEqualTo(1f);
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
    void negativeResilienceNeverRaisesTheCritChanceButRaisesTheCritBonus() {
        StatView brittle = stats(BaseStats.defaults().with(EnemyStat.RESILIENCE, -100f));
        AttackProfile attacker = AttackProfile.critChance(0.4f).withCritMultiplier(2f);

        Damage justInside = HitResolution.resolve(Damage.physical(1000), attacker, brittle, 10000, () -> 0.39);
        Damage justOutside = HitResolution.resolve(Damage.physical(1000), attacker, brittle, 10000, () -> 0.41);

        // the chance stays 40%, the bonus doubles from +100% to +200%
        assertThat(justInside).isEqualTo(new Damage(3000, DamageType.PHYSICAL, true));
        assertThat(justOutside).isEqualTo(Damage.physical(1000));
    }

    @Test
    void periodicDamageNeverCritsEvenWithGuaranteedCritAndFullChance() {
        StatView plain = stats(BaseStats.defaults());
        AttackProfile pulse = AttackProfile.critChance(1f).withGuaranteedCrit().asPeriodic();

        Damage landed = HitResolution.resolve(Damage.physical(1000), pulse, plain, 10000, () -> 0.0);

        assertThat(landed).isEqualTo(Damage.physical(1000));
    }

    @Test
    void aGuaranteedCritLandsBelowFullResilienceWithoutDrawingAndShrinksWithIt() {
        StatView resilient = stats(BaseStats.defaults().with(EnemyStat.RESILIENCE, 70f));
        AttackProfile attacker = AttackProfile.none().withCritMultiplier(2f).withGuaranteedCrit();

        Damage landed = HitResolution.resolve(Damage.physical(1000), attacker, resilient, 10000, () -> {
            throw new IllegalStateException("drew a random number");
        });

        assertThat(landed).isEqualTo(new Damage(1300, DamageType.PHYSICAL, true));
    }

    @Test
    void aGuaranteedCritNeverLandsOnACritImmuneEnemy() {
        StatView immune = stats(BaseStats.defaults().with(EnemyStat.RESILIENCE, 100f));

        Damage landed = HitResolution.resolve(Damage.physical(1000), AttackProfile.none().withGuaranteedCrit(), immune,
                10000, () -> 0.0);

        assertThat(landed).isEqualTo(Damage.physical(1000));
    }

    @Test
    void critDamageBonusesAddToTheMultiplierInsteadOfReplacingIt() {
        AttackProfile sniper = AttackProfile.none().withCritMultiplier(2f).withCritDamageBonus(0.5f);

        assertThat(sniper.critMultiplier()).isEqualTo(2.5f);
        assertThat(HitResolution.critFactor(sniper, stats(BaseStats.defaults()))).isEqualTo(2.5f);
    }

    @Test
    void critFactorAgainstACritImmuneEnemyIsOne() {
        StatView immune = stats(BaseStats.defaults().with(EnemyStat.RESILIENCE, 100f));

        assertThat(HitResolution.critFactor(AttackProfile.none().withCritMultiplier(3f), immune)).isEqualTo(1f);
    }

    @Test
    void glancingPenetrationHelpsAShotThatDoesNotCritButNotOneThatDoes() {
        StatView armored = stats(BaseStats.defaults().with(EnemyStat.ARMOR, 100f).with(EnemyStat.PHYSICAL_PLATING, 300f));
        AttackProfile weakSpot = AttackProfile.critChance(0.5f).withGlancingPenetration(100f, 1f);

        Damage glancing = HitResolution.resolve(Damage.physical(1000), weakSpot, armored, 10000, () -> 0.9);
        Damage crit = HitResolution.resolve(Damage.physical(1000), weakSpot, armored, 10000, () -> 0.1);

        assertThat(glancing).isEqualTo(Damage.physical(1000));
        assertThat(crit.critical()).isTrue();
        assertThat(crit.amount()).isEqualTo(Math.round(1000 * 1.5f * 100f / 200f - 300f));
    }

    @Test
    void shieldingTakesItsShareOfAHitAfterDamageTaken() {
        StatView shielded = stats(BaseStats.defaults().with(EnemyStat.PHYSICAL_SHIELDING, 0.4f)
                .with(EnemyStat.PHYSICAL_DAMAGE_TAKEN, 1.5f));

        assertThat(HitResolution.resolve(Damage.physical(1000), shielded, 10000)).isEqualTo(Damage.physical(900));
        assertThat(HitResolution.resolve(Damage.magic(1000), shielded, 10000)).isEqualTo(Damage.magic(1000));
    }

    @Test
    void aShieldCountsTowardsWhatAnEnemyShrugsOff() {
        StatView shielded = stats(BaseStats.defaults().with(EnemyStat.PHYSICAL_SHIELDING, 0.4f));

        assertThat(HitResolution.reductionAgainst(DamageType.PHYSICAL, shielded)).isCloseTo(0.4f, within(1e-6f));
        assertThat(HitResolution.reductionAgainst(DamageType.MAGIC, shielded)).isZero();
    }

    @Test
    void aCritFromAnAttackerThatPiercesShieldsGoesStraightToHealth() {
        StatView shielded = stats(BaseStats.defaults().with(EnemyStat.PHYSICAL_SHIELDING, 0.5f));
        AttackProfile cleanShot = AttackProfile.critChance(0.5f).withCritsPierceShields();

        Damage crit = HitResolution.resolve(Damage.physical(1000), cleanShot, shielded, 10000, () -> 0.0);
        Damage plain = HitResolution.resolve(Damage.physical(1000), cleanShot, shielded, 10000, () -> 0.99);

        assertThat(crit.amount()).isEqualTo(1500);
        assertThat(plain.amount()).isEqualTo(500);
    }

    @Test
    void damageThatTicksTakesTheOverTimeMultiplierAndAHitDoesNot() {
        td.stat.BaseStats stats = td.stat.BaseStats.defaults().with(td.stat.EnemyStat.PERIODIC_DAMAGE_TAKEN, 1.5f);
        td.stat.StatView view = new td.stat.StatSheet(stats, accumulator -> {
        });

        td.damage.Damage ticking = HitResolution.resolve(td.damage.Damage.magic(1000),
                td.damage.AttackProfile.none().asPeriodic(), view, 100000, () -> 1.0);
        td.damage.Damage hit = HitResolution.resolve(td.damage.Damage.magic(1000), td.damage.AttackProfile.none(),
                view, 100000, () -> 1.0);

        org.assertj.core.api.Assertions.assertThat(ticking.amount()).isEqualTo(1500);
        org.assertj.core.api.Assertions.assertThat(hit.amount()).isEqualTo(1000);
    }
}
