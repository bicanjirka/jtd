package td.tower.seeker;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class BroodSpecTest {

    private static List<MissileKind> firstSix(BroodSpec brood) {
        return IntStream.rangeClosed(1, 6).mapToObj(brood::kindOf).toList();
    }

    @Test
    void frostAloneMakesOnlyFrost() {
        assertThat(firstSix(BroodSpec.frostOnly())).containsOnly(MissileKind.FROST);
    }

    @Test
    void aSecondKindComesEveryThirdMissile() {
        BroodSpec brood = BroodSpec.frostOnly().withSecond(MissileKind.RUBY);

        assertThat(firstSix(brood)).containsExactly(MissileKind.FROST, MissileKind.FROST, MissileKind.RUBY,
                MissileKind.FROST, MissileKind.FROST, MissileKind.RUBY);
    }

    @Test
    void aRichBroodBringsTheSecondKindEverySecondMissile() {
        BroodSpec brood = BroodSpec.frostOnly().withSecond(MissileKind.SHADE).richer();

        assertThat(firstSix(brood)).containsExactly(MissileKind.FROST, MissileKind.SHADE, MissileKind.FROST,
                MissileKind.SHADE, MissileKind.FROST, MissileKind.SHADE);
    }

    @Test
    void onlyFrostAndTheSecondKindOwnedMayStayInTheNest() {
        BroodSpec brood = BroodSpec.frostOnly().withSecond(MissileKind.SHADE);

        assertThat(brood.owns(MissileKind.FROST)).isTrue();
        assertThat(brood.owns(MissileKind.SHADE)).isTrue();
        assertThat(brood.owns(MissileKind.RUBY)).isFalse();
    }
}
