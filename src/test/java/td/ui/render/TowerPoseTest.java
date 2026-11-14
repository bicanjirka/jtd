package td.ui.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TowerPoseTest {

    @Test
    void cuesCombineAndACueHeldTwiceIsHeldOnce() {
        TowerPose pose = TowerPose.idle().with(TowerCue.SILVER_ROUND).with(TowerCue.HOLLOW_POINT)
                .with(TowerCue.SILVER_ROUND);

        assertThat(pose.cues()).containsExactlyInAnyOrder(TowerCue.SILVER_ROUND, TowerCue.HOLLOW_POINT);
    }

    @Test
    void aCueThatIsNotHeldLeavesThePoseAsItWas() {
        TowerPose pose = TowerPose.idle().with(TowerCue.STREAMING, false);

        assertThat(pose).isSameAs(TowerPose.idle());
    }

    @Test
    void aShotThrowsThePartBackAndTheReloadEasesItForward() {
        TowerPose fired = TowerPose.idle().reloading(0f);
        TowerPose halfway = TowerPose.idle().reloading(0.5f);
        TowerPose ready = TowerPose.idle().reloading(1f);

        assertThat(fired.recoil()).isEqualTo(1f);
        assertThat(halfway.recoil()).isStrictlyBetween(0f, 1f);
        assertThat(ready.recoil()).isZero();
    }

    @Test
    void theFlashShowsOnlyOverTheFirstMomentsOfAReload() {
        TowerPose fired = TowerPose.idle().reloading(0f);
        TowerPose halfway = TowerPose.idle().reloading(0.5f);

        assertThat(fired.flash()).isTrue();
        assertThat(halfway.flash()).isFalse();
    }

    @Test
    void aRefillingPartIsNeitherThrownBackNorFlashing() {
        TowerPose refilling = TowerPose.idle().refilling(0.2f);

        assertThat(refilling.recoil()).isZero();
        assertThat(refilling.flash()).isFalse();
        assertThat(refilling.reload()).isEqualTo(0.2f);
    }

    @Test
    void snappingMovesRecoilAndReloadToTheNearestStepSoNearbyPosesAreEqual() {
        TowerPose one = TowerPose.idle().refilling(0.52f).snapped(4);
        TowerPose other = TowerPose.idle().refilling(0.46f).snapped(4);

        assertThat(one).isEqualTo(other);
        assertThat(one.reload()).isEqualTo(0.5f);
    }

    @Test
    void aPoseAlreadyOnAStepIsItsOwnSnap() {
        TowerPose pose = TowerPose.idle().with(TowerCue.CHARGED);

        assertThat(pose.snapped(4)).isSameAs(pose);
    }
}
