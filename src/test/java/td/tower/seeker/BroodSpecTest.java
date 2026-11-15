package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.util.RandomSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BroodSpecTest {

    @Test
    void frostAloneDrawsFrostWithoutRolling() {
        RandomSource never = () -> {
            throw new AssertionError("rolled");
        };

        assertThat(BroodSpec.frostOnly().draw(never)).isEqualTo(MissileKind.FROST);
    }

    @Test
    void aSecondKindComesUpOneInThree() {
        BroodSpec brood = BroodSpec.frostOnly().withSecond(MissileKind.RUBY);

        assertThat(brood.draw(() -> 0.32)).isEqualTo(MissileKind.RUBY);
        assertThat(brood.draw(() -> 0.34)).isEqualTo(MissileKind.FROST);
        assertThat(brood.chances().get(MissileKind.RUBY)).isCloseTo(1f / 3f, within(1e-6f));
    }

    @Test
    void aRichBroodBringsTheSecondKindUpOneInTwo() {
        Map<MissileKind, Float> chances = BroodSpec.frostOnly().withSecond(MissileKind.SHADE).richer().chances();

        assertThat(chances).containsEntry(MissileKind.SHADE, 0.5f).containsEntry(MissileKind.FROST, 0.5f);
    }

    @Test
    void onlyFrostAndTheSecondKindOwnedMayStayInTheNest() {
        BroodSpec brood = BroodSpec.frostOnly().withSecond(MissileKind.SHADE);

        assertThat(brood.owns(MissileKind.FROST)).isTrue();
        assertThat(brood.owns(MissileKind.SHADE)).isTrue();
        assertThat(brood.owns(MissileKind.RUBY)).isFalse();
    }
}
