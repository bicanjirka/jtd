package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArcLengthPathTest {

    // Steps are deliberately chosen outside the 1x1 grid below, so finalise() never
    // dereferences a (deliberately unpopulated) grid cell - see PathNormalTest for the
    // same convention.

    @Test
    void aPathWithFewerThanTwoPointsHasNoArcLength() {
        PathNormal path = new PathNormal(1);
        path.addStep(5, 5);
        path.finalise(new RecordingCell[1][1]);

        assertThat(ArcLengthPath.of(path)).isEmpty();
    }

    @Test
    void aPathWithZeroTotalLengthHasNoArcLength() {
        PathNormal path = new PathNormal(1);
        path.addStep(5, 5);
        path.addStep(5, 5);
        path.finalise(new RecordingCell[1][1]);

        assertThat(ArcLengthPath.of(path)).isEmpty();
    }

    @Test
    void totalLengthIsTheSumOfEuclideanSegmentLengths() {
        PathNormal path = new PathNormal(1);
        path.addStep(5, 5);
        path.addStep(8, 9);
        path.finalise(new RecordingCell[1][1]);

        ArcLengthPath arcLengthPath = ArcLengthPath.of(path).orElseThrow();

        assertThat(arcLengthPath.totalLength()).isEqualTo(5.0);
    }

    @Test
    void poseAtZeroIsTheFirstPointFacingTheFirstSegment() {
        PathNormal path = new PathNormal(1);
        path.addStep(5, 5);
        path.addStep(15, 5);
        path.finalise(new RecordingCell[1][1]);

        PathPose pose = ArcLengthPath.of(path).orElseThrow().poseAt(0);

        assertThat(pose.position()).isEqualTo(new Vec2(5, 5));
        assertThat(pose.facingRadians()).isEqualTo(0.0);
    }

    @Test
    void poseAtInterpolatesWithinTheContainingSegment() {
        PathNormal path = new PathNormal(1);
        path.addStep(5, 5);
        path.addStep(15, 5);
        path.addStep(15, 15);
        path.finalise(new RecordingCell[1][1]);

        ArcLengthPath arcLengthPath = ArcLengthPath.of(path).orElseThrow();

        assertThat(arcLengthPath.poseAt(5).position()).isEqualTo(new Vec2(10, 5));
        assertThat(arcLengthPath.poseAt(15).position()).isEqualTo(new Vec2(15, 10));
        assertThat(arcLengthPath.poseAt(15).facingRadians()).isCloseTo(Math.PI / 2, within(1e-9));
    }

    @Test
    void poseAtClampsPastTheEndToTheFinalSegment() {
        PathNormal path = new PathNormal(1);
        path.addStep(5, 5);
        path.addStep(15, 5);
        path.finalise(new RecordingCell[1][1]);

        PathPose pose = ArcLengthPath.of(path).orElseThrow().poseAt(1000);

        assertThat(pose.position()).isEqualTo(new Vec2(15, 5));
    }
}
