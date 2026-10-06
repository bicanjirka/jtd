package td.wave;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArcLengthPathTest {

    @Test
    void onlyTheWalkedStretchCountsAsPassingNearAPoint() {
        ArcLengthPath path = ArcLengthPath.of(new PathNormal(List.of(new Vec2(0, 0), new Vec2(100, 0), new Vec2(100, 100))))
                .orElseThrow();

        assertThat(path.passesWithin(30, 10, 15, 0, 100)).isTrue();
        assertThat(path.passesWithin(30, 10, 15, 60, 200)).isFalse();
        assertThat(path.passesWithin(110, 80, 15, 150, 200)).isTrue();
        assertThat(path.passesWithin(110, 80, 15, 0, 150)).isFalse();
    }

    @Test
    void aPathWithFewerThanTwoPointsHasNoArcLength() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5)));

        assertThat(ArcLengthPath.of(path)).isEmpty();
    }

    @Test
    void aPathWithZeroTotalLengthHasNoArcLength() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5), new Vec2(5, 5)));

        assertThat(ArcLengthPath.of(path)).isEmpty();
    }

    @Test
    void totalLengthIsTheSumOfEuclideanSegmentLengths() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5), new Vec2(8, 9)));

        ArcLengthPath arcLengthPath = ArcLengthPath.of(path).orElseThrow();

        assertThat(arcLengthPath.totalLength()).isEqualTo(5.0);
    }

    @Test
    void poseAtZeroIsTheFirstPointFacingTheFirstSegment() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5)));

        PathPose pose = ArcLengthPath.of(path).orElseThrow().poseAt(0);

        assertThat(pose.position()).isEqualTo(new Vec2(5, 5));
        assertThat(pose.facingRadians()).isEqualTo(0.0);
    }

    @Test
    void poseAtInterpolatesWithinTheContainingSegment() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5), new Vec2(15, 15)));

        ArcLengthPath arcLengthPath = ArcLengthPath.of(path).orElseThrow();

        assertThat(arcLengthPath.poseAt(5).position()).isEqualTo(new Vec2(10, 5));
        assertThat(arcLengthPath.poseAt(15).position()).isEqualTo(new Vec2(15, 10));
        assertThat(arcLengthPath.poseAt(15).facingRadians()).isCloseTo(Math.PI / 2, within(1e-9));
    }

    @Test
    void poseAtClampsPastTheEndToTheFinalSegment() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5)));

        PathPose pose = ArcLengthPath.of(path).orElseThrow().poseAt(1000);

        assertThat(pose.position()).isEqualTo(new Vec2(15, 5));
    }
}
