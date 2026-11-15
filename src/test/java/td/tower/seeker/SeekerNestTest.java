package td.tower.seeker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeekerNestTest {

    private static final int GAP = 4;

    @Test
    void itHasRoomUntilItHoldsItsCapacity() {
        SeekerNest nest = new SeekerNest();

        nest.load(MissileKind.FROST);
        nest.load(MissileKind.FROST);

        assertThat(nest.hasRoom(3)).isTrue();
        assertThat(nest.hasRoom(2)).isFalse();
    }

    @Test
    void aLaunchTakesTheFrontMissileAndMakesTheNextWaitOutTheGap() {
        SeekerNest nest = new SeekerNest();
        nest.load(MissileKind.RUBY);
        nest.load(MissileKind.FROST);

        MissileKind first = nest.launch(GAP);
        for (int i = 1; i < GAP; i++) {
            nest.tick();
        }
        boolean readyEarly = nest.readyToLaunch();
        nest.tick();

        assertThat(first).isEqualTo(MissileKind.RUBY);
        assertThat(readyEarly).isFalse();
        assertThat(nest.readyToLaunch()).isTrue();
    }

    @Test
    void aHatchingHatchesItsFirstMissileAtOnceAndTheRestAGapApart() {
        SeekerNest nest = new SeekerNest();
        nest.hatch(2);

        boolean first = nest.hatchesNow(GAP);
        int quiet = 0;
        while (!nest.hatchesNow(GAP)) {
            quiet++;
        }

        assertThat(first).isTrue();
        assertThat(quiet).isEqualTo(GAP - 1);
        assertThat(nest.hatchesNow(GAP)).isFalse();
    }

    @Test
    void itLetsGoTheMissilesOfAKindItNoLongerHolds() {
        SeekerNest nest = new SeekerNest();
        nest.load(MissileKind.FROST);
        nest.load(MissileKind.SHADE);

        nest.keepOnly(kind -> kind == MissileKind.FROST);

        assertThat(nest.missiles()).containsExactly(MissileKind.FROST);
    }
}
