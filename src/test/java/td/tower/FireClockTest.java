package td.tower;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FireClockTest {

    private static int shotsOver(FireClock clock, double rate, int ticks) {
        int shots = 0;
        for (int tick = 0; tick < ticks; tick++) {
            int due = clock.shotsDue();
            for (int i = 0; i < due; i++) {
                clock.spend();
            }
            shots += due;
            clock.advance(rate);
        }
        return shots;
    }

    @Test
    void aFreshClockIsReadyAtOnce() {
        assertThat(new FireClock(49).isReady()).isTrue();
    }

    @Test
    void atTheBaseRateAShotComesEveryPeriodExactly() {
        FireClock clock = new FireClock(49);

        assertThat(shotsOver(clock, 1.0, 500)).isEqualTo(10);
    }

    @Test
    void aTenPercentFasterEveryTickTowerHitsTwentyTwoTimesInTwentyTicksOnAverage() {
        FireClock clock = new FireClock(0);

        assertThat(shotsOver(clock, 1.1, 1001)).isEqualTo(1101);
    }

    @Test
    void afterTenTicksAtOnePointOneTheRememberedTenthsPayForASecondHit() {
        FireClock clock = new FireClock(0);
        int[] perTick = new int[12];
        for (int tick = 0; tick < perTick.length; tick++) {
            perTick[tick] = clock.shotsDue();
            for (int i = 0; i < perTick[tick]; i++) {
                clock.spend();
            }
            clock.advance(1.1);
        }

        assertThat(perTick).containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1);
    }

    @Test
    void shotsOverALongRunEqualTheExactRateTimesTimeForManyRatesAndPeriods() {
        double[] rates = {0.25, 1.0 / 0.75, 1.0 / 0.9f, 1.1, 1.5, 1.0 / (1.0 - 0.1f), 2.0, 3.0, 7.35, 10.0};
        int[] coolDowns = {0, 1, 19, 45, 49, 70};
        int ticks = 6000;

        for (int coolDown : coolDowns) {
            for (double rate : rates) {
                int shots = shotsOver(new FireClock(coolDown), rate, ticks);

                long expected = (long) Math.floor(1 + (ticks - 1) * rate / (coolDown + 1) + 1e-6);
                assertThat((long) shots).as("cooldown %d at rate %s", coolDown, rate).isEqualTo(expected);
            }
        }
    }

    @Test
    void aTowerWithNothingToShootAtBanksNoMoreThanOnePeriod() {
        FireClock clock = new FireClock(9);
        for (int tick = 0; tick < 1000; tick++) {
            clock.advance(2.0);
        }

        assertThat(clock.shotsDue()).isEqualTo(1);
    }

    @Test
    void aShortenedWaitMakesTheNextShotComeSooner() {
        FireClock clock = new FireClock(9);
        clock.spend(0.5);
        int wait = 0;
        while (!clock.isReady()) {
            clock.advance(1.0);
            wait++;
        }

        assertThat(wait).isEqualTo(5);
    }

    @Test
    void aLongerWaitMakesTheNextShotComeLater() {
        FireClock clock = new FireClock(9);
        clock.spend(2.0);
        int wait = 0;
        while (!clock.isReady()) {
            clock.advance(1.0);
            wait++;
        }

        assertThat(wait).isEqualTo(20);
    }

    @Test
    void theRemainingWaitFallsFromOneToZero() {
        FireClock clock = new FireClock(9);
        clock.spend();

        assertThat(clock.remaining()).isEqualTo(1f);

        for (int tick = 0; tick < 10; tick++) {
            clock.advance(1.0);
        }

        assertThat(clock.remaining()).isZero();
    }
}
