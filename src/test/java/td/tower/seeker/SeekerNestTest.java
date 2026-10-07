package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;

class SeekerNestTest {

    private static final NestSpec SPEC = NestSpec.of(3, 4);

    @Test
    void itHoldsMissilesUpToItsCapacity() {
        SeekerNest nest = new SeekerNest();

        for (int i = 0; i < 3; i++) {
            nest.load();
        }

        assertThat(nest.stored()).isEqualTo(3);
        assertThat(nest.hasRoom(SPEC)).isFalse();
        assertThat(nest.hasRoom(SPEC.holdingMore(1))).isTrue();
    }

    @Test
    void aLaunchMakesTheNextWaitOutTheGap() {
        SeekerNest nest = new SeekerNest();
        nest.load();
        nest.load();

        nest.launch(SPEC);
        int waited = 0;
        while (!nest.readyToLaunch()) {
            nest.tick(SPEC);
            waited++;
        }

        assertThat(waited).isEqualTo(4);
    }

    @Test
    void anEmptyNestIsNeverReady() {
        assertThat(new SeekerNest().readyToLaunch()).isFalse();
    }

    @Test
    void aSalvoRemembersItsTargetsUntilItHasGoneQuietForTwiceTheGap() {
        SeekerNest nest = new SeekerNest();
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        nest.load();
        nest.launch(SPEC);
        nest.recordTarget(target);

        for (int t = 0; t < 8; t++) {
            nest.tick(SPEC);
        }
        boolean stillRemembered = nest.salvoTargets().contains(target);
        nest.tick(SPEC);

        assertThat(stillRemembered).isTrue();
        assertThat(nest.salvoTargets()).isEmpty();
    }
}
