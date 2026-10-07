package td.tower.buff;

import org.junit.jupiter.api.Test;
import td.stat.DisruptionPenalty;
import td.tower.TowerBaseStats;
import td.tower.TowerStats;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerBuffTest {

    @Test
    void noneIsTheIdentityElementOnBothSides() {
        TowerBuff buff = TowerBuff.amplifying(0.2f);

        assertThat(TowerBuff.none().combine(buff)).isEqualTo(buff);
        assertThat(buff.combine(TowerBuff.none())).isEqualTo(buff);
    }

    @Test
    void combineIsAdditive() {
        TowerBuff a = TowerBuff.amplifying(0.1f);
        TowerBuff b = TowerBuff.amplifying(0.3f);

        TowerBuff combined = a.combine(b);

        assertThat(combined.damageBonus()).isEqualTo(0.4f);
        assertThat(combined.rangeBonus()).isEqualTo(0.4f);
    }

    @Test
void combineIsAdditiveOnBountyToo() {        TowerBuff a = new TowerBuff(0f, 0f, 0f, 0.2f);        TowerBuff b = new TowerBuff(0f, 0f, 0f, 0.1f);        assertThat(a.combine(b).bountyBonus()).isCloseTo(0.3f, org.assertj.core.data.Offset.offset(1e-6f));    }    @Test    void fireRateBonusesEachCutWhatIsLeftOfTheCooldown() {        TowerBuff a = TowerBuff.fireRate(0.3f);        TowerBuff b = TowerBuff.fireRate(0.25f);        TowerBuff combined = a.combine(b);        assertThat(combined.fireRateBonus()).isCloseTo(1f - 0.7f * 0.75f, org.assertj.core.data.Offset.offset(1e-6f));        assertThat(combined.fireRateMultiplier()).isCloseTo(1.0 / (0.7 * 0.75), org.assertj.core.data.Offset.offset(1e-5));    }    @Test    void manyStackedFireRateBonusesNeverRunPastTenTimesTheBaseRate() {        TowerBuff stacked = List.of(TowerBuff.fireRate(0.55f), TowerBuff.fireRate(0.45f), TowerBuff.fireRate(0.45f))                .stream().reduce(TowerBuff.none(), TowerBuff::combine);        assertThat(stacked.fireRateMultiplier()).isGreaterThan(5.0);        assertThat(stacked.combine(TowerBuff.fireRate(0.9f)).fireRateMultiplier()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1e-4));    }

    @Test
    void amplifyingOnlyTouchesDamageAndRange() {
        TowerBuff buff = TowerBuff.amplifying(0.5f);

        assertThat(buff.fireRateBonus()).isZero();
        assertThat(buff.bountyBonus()).isZero();
    }

    @Test
    void reducingAnEmptyStreamYieldsNone() {
        TowerBuff reduced = List.<TowerBuff>of().stream().reduce(TowerBuff.none(), TowerBuff::combine);

        assertThat(reduced).isEqualTo(TowerBuff.none());
    }

    @Test
    void reducingThreeEqualBuffsStacksThemAdditively() {
        List<TowerBuff> buffs = List.of(
                TowerBuff.amplifying(0.2f), TowerBuff.amplifying(0.2f), TowerBuff.amplifying(0.2f));

        TowerBuff reduced = buffs.stream().reduce(TowerBuff.none(), TowerBuff::combine);

        assertThat(reduced.damageBonus()).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(1e-6f));
    }

    @Test
    void damageForAndRangeForApplyTheBonusAsAMultiplier() {
        TowerBuff buff = TowerBuff.amplifying(0.5f);

        assertThat(buff.damageFor(100)).isEqualTo(150);
        assertThat(buff.rangeFor(2f)).isEqualTo(3f);
    }

    @Test
    void damageForRoundsInsteadOfDroppingTheFraction() {
        TowerBuff buff = TowerBuff.amplifying(0.25f);

        assertThat(buff.damageFor(150)).isEqualTo(188);
    }

    @Test
    void fireRateMultiplierCutsTheWaitByTheBonusFraction() {
        TowerBuff buff = new TowerBuff(0f, 0f, 0.5f, 0f);

        assertThat(buff.fireRateMultiplier()).isEqualTo(2.0);
    }

    @Test
    void fireRateMultiplierNeverGoesPastTenTimesTheBase() {
        TowerBuff buff = new TowerBuff(0f, 0f, 0.99f, 0f);

        assertThat(buff.fireRateMultiplier()).isCloseTo(10.0, org.assertj.core.data.Offset.offset(1e-4));
    }

    @Test
    void noBuffLeavesTheFireRateUnchanged() {
        assertThat(TowerBuff.none().fireRateMultiplier()).isEqualTo(1.0);
    }

    @Test
    void theFourArgumentConstructorDefaultsCritChanceBonusToZero() {
        TowerBuff buff = new TowerBuff(0.1f, 0.1f, 0.1f, 0.1f);

        assertThat(buff.critChanceBonus()).isZero();
    }

    @Test
    void combineIsAdditiveOnCritChanceBonusToo() {
        TowerBuff a = new TowerBuff(0f, 0f, 0f, 0f, 0.1f);
        TowerBuff b = new TowerBuff(0f, 0f, 0f, 0f, 0.05f);

        assertThat(a.combine(b).critChanceBonus()).isCloseTo(0.15f, org.assertj.core.data.Offset.offset(1e-6f));
    }

    @Test
    void critChanceForAddsTheBonusToTheBase() {
        TowerBuff buff = new TowerBuff(0f, 0f, 0f, 0f, 0.15f);

        assertThat(buff.critChanceFor(0f)).isCloseTo(0.15f, org.assertj.core.data.Offset.offset(1e-6f));
    }

    @Test
    void critChanceForClampsToAValidProbability() {
        TowerBuff buff = new TowerBuff(0f, 0f, 0f, 0f, 1.5f);

        assertThat(buff.critChanceFor(0f)).isEqualTo(1f);
    }

    @Test
    void stackedPenaltiesNeverTakeFireRateOrRangeBelowAQuarter() {
        TowerBuff crushed = TowerBuff.fireRate(-2f).withRange(-2f);

        assertThat(crushed.fireRateMultiplier()).isCloseTo(1.0 / 1.75, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(crushed.rangeFor(4f)).isEqualTo(1f);
    }

    @Test
    void anAuraBuffAndADisruptionPenaltyAddUp() {
        TowerStats stats = TowerStats.of(new TowerBaseStats(1, 2f, 20), TowerBuff.range(0.5f),
                new DisruptionPenalty(0.25f, 0.2f), 10);

        assertThat(stats.range()).isCloseTo(2f * 1.3f, org.assertj.core.data.Offset.offset(1e-5f));
        assertThat(stats.fireRate()).isCloseTo(1.0 / 1.25, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    void reachForXpKeepsTheBuffedRangeAndIgnoresTheDisruption() {
        TowerStats stats = TowerStats.of(new TowerBaseStats(1, 2f, 20), TowerBuff.range(0.5f),
                new DisruptionPenalty(0.25f, 0.2f), 10);

        assertThat(stats.reachReal()).isCloseTo(2f * 1.5f * 10, org.assertj.core.data.Offset.offset(1e-4f));
    }
}
