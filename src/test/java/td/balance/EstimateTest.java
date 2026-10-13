package td.balance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class EstimateTest {

    @Test
    void aWinRateOfNoneOrAllStillHasAnIntervalInsideZeroToOne() {
        Estimate none = Estimate.proportion(0, 20);
        Estimate all = Estimate.proportion(20, 20);

        assertThat(none.value()).isZero();
        assertThat(none.low()).isZero();
        assertThat(none.high()).isBetween(0.10, 0.20);
        assertThat(all.value()).isEqualTo(1.0);
        assertThat(all.high()).isEqualTo(1.0);
        assertThat(all.low()).isBetween(0.80, 0.90);
    }

    @Test
    void aMeanOfEqualValuesIsCertain() {
        Estimate mean = Estimate.of(new double[]{3, 3, 3, 3});

        assertThat(mean.value()).isEqualTo(3.0);
        assertThat(mean.high() - mean.low()).isZero();
    }

    @Test
    void twoMeansWhoseIntervalsOverlapAreNotDifferent() {
        Estimate low = Estimate.of(new double[]{1, 2, 3, 4});
        Estimate overlapping = Estimate.of(new double[]{2, 3, 4, 5});
        Estimate far = Estimate.of(new double[]{20, 21, 22, 23});

        assertThat(low.differsFrom(overlapping)).isFalse();
        assertThat(low.differsFrom(far)).isTrue();
        assertThat(low.value()).isCloseTo(2.5, within(1e-9));
    }
}
