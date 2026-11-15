package td.tower.seeker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedingMechanicTest {

    private static final double PERIOD = 40.0;

    @Test
    void aFreezeFeedsItsTicksAtTheTowersPace() {
        assertThat(FeedingMechanic.feedFor(0.0, PERIOD, 2.0)).isEqualTo(FeedingMechanic.FED_TICKS * 2.0);
    }

    @Test
    void freezesNeverFeedALoadPastItsFloor() {
        double most = PERIOD * (1.0 - FeedingMechanic.FLOOR);
        double fed = 0.0;
        for (int i = 0; i < 100; i++) {
            fed += FeedingMechanic.feedFor(fed, PERIOD, 1.0);
        }

        assertThat(fed).isEqualTo(most);
        assertThat(FeedingMechanic.feedFor(fed, PERIOD, 1.0)).isZero();
    }
}
