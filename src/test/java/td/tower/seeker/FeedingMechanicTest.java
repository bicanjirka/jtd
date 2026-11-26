package td.tower.seeker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeedingMechanicTest {

    @Test
    void aFreezeFeedsItsTicksAtTheTowersPace() {
        assertThat(FeedingMechanic.feedFor(2.0)).isEqualTo(FeedingMechanic.FED_TICKS * 2.0);
    }
}
