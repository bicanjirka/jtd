package td.tower;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

class TierRiseTest {

    @Test
    void aRiseIsAnsweredOnlyInsideItsWindow() {
        TierRise rise = new TierRise(0, 1, 20);

        assertThat(rise.ticksSince(19, 10)).isEmpty();
        assertThat(rise.ticksSince(24, 10)).isEqualTo(OptionalInt.of(4));
        assertThat(rise.ticksSince(31, 10)).isEmpty();
    }

    @Test
    void noRiseIsNeverAnswered() {
        assertThat(TierRise.none().ticksSince(0, 10)).isEmpty();
    }
}
