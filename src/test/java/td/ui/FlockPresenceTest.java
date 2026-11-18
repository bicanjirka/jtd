package td.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlockPresenceTest {

    @Test
    void birdsComeInOneAfterAnotherOnceTheTowerHasBeenQuietAWhile() {
        FlockPresence coming = FlockPresence.of(FlockPresence.DELAY_SECONDS + FlockPresence.ARRIVE_SECONDS / 2, false,
                0);

        assertThat(FlockPresence.of(FlockPresence.DELAY_SECONDS, false, 0).empty()).isTrue();
        assertThat(coming.in(0, 3)).isGreaterThan(coming.in(2, 3));
        assertThat(coming.out(0.5)).isZero();
    }

    @Test
    void aFightScattersEveryBirdOverItsLeaving() {
        double quiet = FlockPresence.DELAY_SECONDS + FlockPresence.ARRIVE_SECONDS;

        FlockPresence starting = FlockPresence.of(quiet, true, 0);
        FlockPresence over = FlockPresence.of(quiet, true, FlockPresence.LEAVE_SECONDS * 2);

        assertThat(starting.empty()).isFalse();
        assertThat(starting.out(1)).isPositive();
        assertThat(over.empty()).isTrue();
        assertThat(over.out(0)).isEqualTo(1);
    }
}
