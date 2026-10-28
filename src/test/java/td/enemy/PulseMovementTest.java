package td.enemy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class PulseMovementTest {

    @Test
    void aSwellPeaksByTheGainMidwayAndSettlesBackToNormalSize() {
        PulseMovement pulse = new PulseMovement(0.5f, 20, 0);

        assertThat(pulse.scaleAt(0)).isEqualTo(1f);
        assertThat(pulse.scaleAt(10)).isCloseTo(1.5f, offset(1e-5f));
        assertThat(pulse.scaleAt(20)).isEqualTo(1f);
    }

    @Test
    void theBodyRestsAtNormalSizeBetweenSwellsThenSwellsAgain() {
        PulseMovement pulse = new PulseMovement(0.5f, 20, 10);

        assertThat(pulse.scaleAt(25)).isEqualTo(1f);
        assertThat(pulse.scaleAt(40)).isCloseTo(1.5f, offset(1e-5f));
    }

    @Test
    void aNegativeGainShrinksTheBodyInstead() {
        assertThat(new PulseMovement(-0.5f, 20, 0).scaleAt(10)).isLessThan(1f);
    }

    @Test
    void aPulseThatCouldNeverSwellIsRejected() {
        assertThatThrownBy(() -> new PulseMovement(-1f, 20, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PulseMovement(0.5f, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PulseMovement(0.5f, 20, -1)).isInstanceOf(IllegalArgumentException.class);
    }
}
