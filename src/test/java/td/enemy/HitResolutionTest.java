package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.AttackProfile;
import td.damage.Damage;
import td.damage.DamageType;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatView;

import static org.assertj.core.api.Assertions.assertThat;

class HitResolutionTest {

    private static StatView stats(BaseStats<EnemyStat> base) {
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
        StatView armored = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 100f));

        assertThat(HitResolution.resolve(Damage.physical(1000), armored)).isEqualTo(Damage.physical(500));
        assertThat(HitResolution.resolve(Damage.magic(1000), armored)).isEqualTo(Damage.magic(1000));
    }

    @Test
    void platingComesOffAfterArmorAndNeverBelowZero() {
        StatView plated = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 100f).with(EnemyStat.PLATING, 300f));

        assertThat(HitResolution.resolve(Damage.physical(1000), plated)).isEqualTo(Damage.physical(200));
        assertThat(HitResolution.resolve(Damage.physical(500), plated)).isEqualTo(Damage.physical(0));
    }

    @Test
    void damageTakenMultipliesLast() {
        StatView vulnerable = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.MAGIC_DAMAGE_TAKEN, 1.5f));

        assertThat(HitResolution.resolve(Damage.magic(100), vulnerable)).isEqualTo(Damage.magic(150));
    }

    @Test
    void aCriticalHitStaysCriticalThroughMitigation() {
        StatView armored = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 100f));

        assertThat(HitResolution.resolve(new Damage(1000, DamageType.PHYSICAL, true), armored).critical())
                .isTrue();
    }
    @Test
    void fullResilienceNeverLetsACritLand() {
        StatView resilient = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.RESILIENCE, 100f));

        Damage landed = HitResolution.resolve(Damage.physical(1000), AttackProfile.critChance(1f), resilient,
                () -> 0.0);

        assertThat(landed).isEqualTo(Damage.physical(1000));
    }

    @Test
    void halfResilienceHalvesBothTheCritChanceAndTheCritBonus() {
        StatView halfResilient = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.RESILIENCE, 50f));
        AttackProfile attacker = AttackProfile.critChance(0.8f).withCritMultiplier(3f);

        Damage justInside = HitResolution.resolve(Damage.physical(1000), attacker, halfResilient, () -> 0.39);
        Damage justOutside = HitResolution.resolve(Damage.physical(1000), attacker, halfResilient, () -> 0.41);

        assertThat(justInside).isEqualTo(new Damage(2000, DamageType.PHYSICAL, true));
        assertThat(justOutside).isEqualTo(Damage.physical(1000));
    }

    @Test
    void anAttackerWithoutCritChanceNeverDrawsFromTheRandomSource() {
        StatView plain = stats(BaseStats.defaults(EnemyStat.class));

        HitResolution.resolve(Damage.physical(1000), AttackProfile.none(), plain, () -> {
            throw new IllegalStateException("drew a random number");
        });
    }

    @Test
    void penetrationLowersArmorButNeverBelowZero() {
        StatView armored = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 100f));

        Damage halfPierced = HitResolution.resolve(Damage.physical(1000),
                AttackProfile.none().withArmorPenetration(0.5f, 0f), armored, () -> 1.0);
        Damage overPierced = HitResolution.resolve(Damage.physical(1000),
                AttackProfile.none().withArmorPenetration(0.5f, 500f), armored, () -> 1.0);

        assertThat(halfPierced.amount()).isEqualTo(Math.round(1000 * 100f / 150f));
        assertThat(overPierced).isEqualTo(Damage.physical(1000));
    }

    @Test
    void negativeResilienceRaisesBothTheCritChanceAndTheCritBonus() {
        StatView brittle = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.RESILIENCE, -50f));
        AttackProfile attacker = AttackProfile.critChance(0.2f).withCritMultiplier(2f);

        Damage justInside = HitResolution.resolve(Damage.physical(1000), attacker, brittle, () -> 0.29);
        Damage justOutside = HitResolution.resolve(Damage.physical(1000), attacker, brittle, () -> 0.31);

        // the chance rises from 20% to 30%, the bonus from +100% to +150%
        assertThat(justInside).isEqualTo(new Damage(2500, DamageType.PHYSICAL, true));
        assertThat(justOutside).isEqualTo(Damage.physical(1000));
    }

    @Test
    void periodicDamageNeverCritsEvenWithGuaranteedCritAndFullChance() {
        StatView plain = stats(BaseStats.defaults(EnemyStat.class));
        AttackProfile pulse = AttackProfile.critChance(1f).withGuaranteedCrit().asPeriodic();

        Damage landed = HitResolution.resolve(Damage.physical(1000), pulse, plain, () -> 0.0);

        assertThat(landed).isEqualTo(Damage.physical(1000));
    }

    @Test
    void aGuaranteedCritLandsBelowFullResilienceWithoutDrawingAndShrinksWithIt() {
        StatView resilient = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.RESILIENCE, 70f));
        AttackProfile attacker = AttackProfile.none().withCritMultiplier(2f).withGuaranteedCrit();

        Damage landed = HitResolution.resolve(Damage.physical(1000), attacker, resilient, () -> {
            throw new IllegalStateException("drew a random number");
        });

        assertThat(landed).isEqualTo(new Damage(1300, DamageType.PHYSICAL, true));
    }

    @Test
    void aGuaranteedCritNeverLandsOnACritImmuneEnemy() {
        StatView immune = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.RESILIENCE, 100f));

        Damage landed = HitResolution.resolve(Damage.physical(1000), AttackProfile.none().withGuaranteedCrit(), immune, () -> 0.0);

        assertThat(landed).isEqualTo(Damage.physical(1000));
    }

    @Test
    void critDamageBonusesAddToTheMultiplierInsteadOfReplacingIt() {
        AttackProfile sniper = AttackProfile.none().withCritMultiplier(2f).withCritDamageBonus(0.5f);

        assertThat(sniper.critMultiplier()).isEqualTo(2.5f);
        assertThat(HitResolution.critFactor(sniper, stats(BaseStats.defaults(EnemyStat.class)))).isEqualTo(2.5f);
    }

    @Test
    void critFactorAgainstACritImmuneEnemyIsOne() {
        StatView immune = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.RESILIENCE, 100f));

        assertThat(HitResolution.critFactor(AttackProfile.none().withCritMultiplier(3f), immune)).isEqualTo(1f);
    }

    @Test
    void glancingPenetrationHelpsAShotThatDoesNotCritButNotOneThatDoes() {
        StatView armored = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.ARMOR, 100f).with(EnemyStat.PLATING, 300f));
        AttackProfile weakSpot = AttackProfile.critChance(0.5f).withGlancingPenetration(100f, 1f);

        Damage glancing = HitResolution.resolve(Damage.physical(1000), weakSpot, armored, () -> 0.9);
        Damage crit = HitResolution.resolve(Damage.physical(1000), weakSpot, armored, () -> 0.1);

        assertThat(glancing).isEqualTo(Damage.physical(1000));
        assertThat(crit.critical()).isTrue();
        assertThat(crit.amount()).isEqualTo(Math.round(1000 * 1.5f * 100f / 200f - 300f));
    }

    @Test
    void damageThatTicksTakesTheOverTimeMultiplierAndAHitDoesNot() {
        td.stat.BaseStats<EnemyStat> stats = td.stat.BaseStats.defaults(td.stat.EnemyStat.class).with(td.stat.EnemyStat.PERIODIC_DAMAGE_TAKEN, 1.5f);
        td.stat.StatView view = new td.stat.StatSheet(stats, accumulator -> {
        });

        td.damage.Damage ticking = HitResolution.resolve(td.damage.Damage.magic(1000),
                td.damage.AttackProfile.none().asPeriodic(), view, () -> 1.0);
        td.damage.Damage hit = HitResolution.resolve(td.damage.Damage.magic(1000), td.damage.AttackProfile.none(),
                view, () -> 1.0);

        org.assertj.core.api.Assertions.assertThat(ticking.amount()).isEqualTo(1500);
        org.assertj.core.api.Assertions.assertThat(hit.amount()).isEqualTo(1000);
    }
    @Test
    void dodgeChanceFollowsTheArmorCurveAndNeverReachesCertainty() {
        assertThat(HitResolution.dodgeChance(25f)).isEqualTo(0.2f);
        assertThat(HitResolution.dodgeChance(100f)).isEqualTo(0.5f);
        assertThat(HitResolution.dodgeChance(1_000_000f)).isLessThan(1f);
        assertThat(HitResolution.dodgeChance(-50f)).isZero();
    }

    @Test
    void onlyAnAimedHitCanBeDodged() {
        StatView agile = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.AGILITY, 100f));

        assertThat(HitResolution.dodges(AttackProfile.none().asAimed(), agile, () -> 0.49)).isTrue();
        assertThat(HitResolution.dodges(AttackProfile.none().asAimed(), agile, () -> 0.51)).isFalse();
        assertThat(HitResolution.dodges(AttackProfile.none(), agile, () -> 0.0)).isFalse();
        assertThat(HitResolution.dodges(AttackProfile.none().asAimed().asPeriodic(), agile, () -> 0.0)).isFalse();
    }

    @Test
    void accuracyCancelsAgilityPointForPointAndNeverPastZero() {
        StatView agile = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.AGILITY, 100f));

        boolean halfCancelled = HitResolution.dodges(AttackProfile.none().asAimed().withAccuracy(50f), agile, () -> 0.32);
        boolean overCancelled = HitResolution.dodges(AttackProfile.none().asAimed().withAccuracy(500f), agile, () -> 0.0);

        assertThat(halfCancelled).isTrue();
        assertThat(overCancelled).isFalse();
    }

    @Test
    void aGuaranteedCritIsNeverDodged() {
        StatView agile = stats(BaseStats.defaults(EnemyStat.class).with(EnemyStat.AGILITY, 100f));

        assertThat(HitResolution.dodges(AttackProfile.none().asAimed().withGuaranteedCrit(), agile, () -> 0.0)).isFalse();
    }

    @Test
    void aTargetThatCannotDodgeDrawsNothingFromTheRandomSource() {
        StatView clumsy = stats(BaseStats.defaults(EnemyStat.class));
        int[] draws = {0};

        HitResolution.dodges(AttackProfile.none().asAimed(), clumsy, () -> {
            draws[0]++;
            return 0.0;
        });

        assertThat(draws[0]).isZero();
    }
}
