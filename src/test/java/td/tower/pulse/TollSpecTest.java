package td.tower.pulse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TollSpecTest {

    @Test
    void withoutAttuneThereIsNoToll() {
        assertThat(TollSpec.none().isActive()).isFalse();
    }

    @Test
    void buildingTwiceAsFastHalvesTheTicksPerStack() {
        assertThat(new TollSpec(5, 1f, 20).ticksPerStack()).isEqualTo(20);
        assertThat(new TollSpec(5, 2f, 20).ticksPerStack()).isEqualTo(10);
    }
}
