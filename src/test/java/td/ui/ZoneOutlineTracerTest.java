package td.ui;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import td.ui.render.ZoneArcDraw;
import td.ui.render.ZoneDraw;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneOutlineTracerTest {

    private static final Offset<Float> NEAR = Offset.offset(0.001f);
    private static final float RADIUS = 10f;
    private static final float FULL_TURN = (float) Math.TAU;

    private static ZoneDraw disc(float x, float y, float radius) {
        return new ZoneDraw(x, y, radius, 1f, 0f);
    }

    private static float totalSweep(List<ZoneArcDraw> arcs) {
        return (float) arcs.stream().mapToDouble(ZoneArcDraw::sweepRadians).sum();
    }

    @Test
    void discsThatDoNotTouchAreEachOutlinedAllTheWayRound() {
        List<ZoneArcDraw> arcs = ZoneOutlineTracer.trace(List.of(disc(0, 0, RADIUS), disc(100, 0, RADIUS)));

        assertThat(arcs).hasSize(2);
        assertThat(arcs).allSatisfy(arc -> assertThat(arc.sweepRadians()).isCloseTo(FULL_TURN, NEAR));
    }

    @Test
    void aRimLosesTheStretchInsideADiscOverlappingItAcrossTheZeroAngle() {
        List<ZoneArcDraw> arcs = ZoneOutlineTracer.trace(List.of(disc(0, 0, RADIUS), disc(RADIUS, 0, RADIUS)));

        ZoneArcDraw first = arcs.getFirst();
        float thirdOfATurn = FULL_TURN / 3;
        assertThat(first.startRadians()).isCloseTo(thirdOfATurn / 2, NEAR);
        assertThat(first.sweepRadians()).isCloseTo(2 * thirdOfATurn, NEAR);
    }

    @Test
    void stretchesCoveredByTwoDiscsThatOverlapCountOnce() {
        ZoneDraw middle = disc(0, 0, RADIUS);
        List<ZoneDraw> withOne = List.of(middle, disc(RADIUS, 0, RADIUS));
        List<ZoneDraw> withBoth = List.of(middle, disc(RADIUS, 0, RADIUS), disc(RADIUS, 0.5f, RADIUS));

        float sweepWithOne = ZoneOutlineTracer.trace(withOne).getFirst().sweepRadians();
        float sweepWithBoth = ZoneOutlineTracer.trace(withBoth).getFirst().sweepRadians();

        assertThat(sweepWithBoth).isLessThan(sweepWithOne);
        assertThat(sweepWithBoth).isGreaterThan(sweepWithOne - FULL_TURN / 6);
    }

    @Test
    void noStretchOfTheOutlineLiesInsideAnyDisc() {
        List<ZoneDraw> discs = List.of(disc(0, 0, RADIUS), disc(12, 3, RADIUS), disc(6, 14, 8f), disc(-9, 6, 6f));

        List<ZoneArcDraw> arcs = ZoneOutlineTracer.trace(discs);

        assertThat(arcs).isNotEmpty().allSatisfy(arc -> {
            double middle = arc.startRadians() + arc.sweepRadians() / 2.0;
            double x = arc.centerX() + arc.radius() * Math.cos(middle);
            double y = arc.centerY() + arc.radius() * Math.sin(middle);
            assertThat(discs).noneMatch(disc -> Math.hypot(x - disc.centerX(), y - disc.centerY()) < disc.radius() - 0.01);
        });
    }

    @Test
    void aDiscInsideAnotherAddsNothingToTheOutline() {
        List<ZoneArcDraw> arcs = ZoneOutlineTracer.trace(List.of(disc(0, 0, 2f), disc(0, 0, RADIUS)));

        assertThat(arcs).singleElement().satisfies(arc -> assertThat(arc.radius()).isEqualTo(RADIUS));
    }

    @Test
    void twoIdenticalDiscsDrawOneRim() {
        List<ZoneArcDraw> arcs = ZoneOutlineTracer.trace(List.of(disc(5, 5, RADIUS), disc(5, 5, RADIUS)));

        assertThat(totalSweep(arcs)).isCloseTo(FULL_TURN, NEAR);
    }

    @Test
    void noDiscsHaveNoOutline() {
        assertThat(ZoneOutlineTracer.trace(List.of())).isEmpty();
    }
}
