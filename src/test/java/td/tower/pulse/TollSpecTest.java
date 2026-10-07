package td.tower.pulse;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TollSpecTest {

    @Test
    void withoutAttuneThereIsNoToll() {
        assertThat(TollSpec.none().isActive()).isFalse();
    }

    @Test
    void attunesTollStacksToFiveOneASecondAndFadesAfterASecond() {
        TollSpec toll = TollSpec.attuned();

        assertThat(toll.isActive()).isTrue();
        assertThat(toll.cap()).isEqualTo(5);
        assertThat(toll.ticksPerStack()).isEqualTo(20);
        assertThat(toll.fadeTicks()).isEqualTo(20);
    }

    @Test
    void buildingTwiceAsFastHalvesTheTicksPerStack() {
        assertThat(TollSpec.attuned().buildingFasterBy(2f).ticksPerStack()).isEqualTo(10);
    }

    @Test
    void lingeringAddsToTheFadeAndACapReplacesTheCap() {
        TollSpec toll = TollSpec.attuned().lingeringLonger(20).withCap(10);

        assertThat(toll.fadeTicks()).isEqualTo(40);
        assertThat(toll.cap()).isEqualTo(10);
    }
}
