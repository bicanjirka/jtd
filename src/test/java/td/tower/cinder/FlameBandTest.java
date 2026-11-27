package td.tower.cinder;

import org.junit.jupiter.api.Test;
import td.tower.targeting.Viewpoint;

import static org.assertj.core.api.Assertions.assertThat;

class FlameBandTest {

    private static final double HALF = 0.5;

    private final CinderSpec bare = CinderSpec.from(new Viewpoint(0, 0, 100, 32));

    private CinderSpec banded() {
        return this.bare.withCore(new FlameBand(0.2f, 0.4f)).withMiddle(new FlameBand(0.6f, 0.5f));
    }

    @Test
    void aConeWithNoBandsPoursItsFlameOnceAcrossItsWidth() {
        assertThat(this.bare.flameAt(0, HALF)).isEqualTo(1f);
        assertThat(this.bare.flameAt(HALF, HALF)).isEqualTo(1f);
    }

    @Test
    void anEnemyInBothBandsTakesTheMoreOfEach() {
        assertThat(this.banded().flameAt(0.05, HALF)).isEqualTo(1.9f);
    }

    @Test
    void anEnemyOutsideTheInnerBandTakesOnlyTheOuterBandsMore() {
        assertThat(this.banded().flameAt(0.2, HALF)).isEqualTo(1.5f);
    }

    @Test
    void anEnemyOutsideEveryBandTakesTheConesFlameOnce() {
        assertThat(this.banded().flameAt(0.4, HALF)).isEqualTo(1f);
    }

    @Test
    void aBandHoldsAnEnemyOnItsEdge() {
        FlameBand band = new FlameBand(0.5f, 0.4f);

        assertThat(band.moreAt(0.25, HALF)).isEqualTo(0.4f);
        assertThat(band.moreAt(0.2501, HALF)).isZero();
    }
}
