package td.tower;

import org.junit.jupiter.api.Test;
import td.damage.DamageUnits;
import td.util.TickRate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class WaveDamageTallyTest {

    private static final int SECOND = Math.round(TickRate.TICKS_PER_SECOND);

    @Test
    void theRateIsWhatWasDealtOverTheSpanFromFirstToLastDamage() {
        WaveDamageTally tally = WaveDamageTally.none().plus(1000, 1, 100).plus(1000, 1, 100 + 2 * SECOND);

        double rate = tally.pointsPerSecondIn(1).orElseThrow();

        assertThat(rate).isCloseTo(DamageUnits.inPoints(1000) / 2f, within(0.01));
    }

    @Test
    void aSteadyBeatReadsWhatItDealsHoweverFewHitsHaveLanded() {
        int beat = 2 * SECOND;
        WaveDamageTally tally = WaveDamageTally.none().plus(1000, 1, 100);
        double truth = DamageUnits.inPoints(1000) / 2.0;

        for (int hit = 1; hit <= 9; hit++) {
            tally = tally.plus(1000, 1, 100 + hit * beat);

            assertThat(tally.pointsPerSecondIn(1).orElseThrow()).isCloseTo(truth, within(0.01));
        }
    }

    @Test
    void everyHitOfTheFirstVolleyOpensTheSpanAndEveryHitOfALaterOneCounts() {
        WaveDamageTally firstVolley = WaveDamageTally.none().plus(1000, 1, 100).plus(1000, 1, 100).plus(1000, 1, 100);

        WaveDamageTally tally = firstVolley.plus(1000, 1, 100 + SECOND).plus(1000, 1, 100 + SECOND).plus(1000, 1, 100 + SECOND);

        assertThat(tally.pointsPerSecondIn(1).orElseThrow()).isCloseTo(DamageUnits.inPoints(3000), within(0.01));
    }

    @Test
    void hitsCloserThanASecondGiveNoRate() {
        WaveDamageTally tally = WaveDamageTally.none().plus(1000, 1, 100).plus(1000, 1, 100 + SECOND - 1);

        assertThat(tally.pointsPerSecondIn(1)).isEmpty();
    }

    @Test
    void aLaterWaveHidesTheRateOfTheOneBeforeUntilItHasItsOwn() {
        WaveDamageTally last = WaveDamageTally.none().plus(1000, 1, 100).plus(1000, 1, 100 + SECOND);

        assertThat(last.pointsPerSecondIn(2)).isEmpty();
    }

    @Test
    void damageInALaterWaveStartsTheTallyOver() {
        WaveDamageTally tally = WaveDamageTally.none().plus(9000, 1, 100).plus(9000, 1, 100 + SECOND)
                .plus(1000, 2, 500).plus(1000, 2, 500 + SECOND);

        double rate = tally.pointsPerSecondIn(2).orElseThrow();

        assertThat(rate).isCloseTo(DamageUnits.inPoints(1000), within(0.01));
    }
}
