package td.tower;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class TurretAimTest {

    private static final double MAX_TURN = 0.3;

    @Test
    void angleToPointsAlongPositiveXAtZeroRadians() {
        assertThat(TurretAim.angleTo(0, 0, 10, 0)).isCloseTo(0.0, within(1e-9));
    }

    @Test
    void angleToIncreasesTowardPositiveY() {
        assertThat(TurretAim.angleTo(0, 0, 0, 10)).isCloseTo(Math.PI / 2, within(1e-9));
    }

    @Test
    void oneTickTurnsTowardTheDesiredAngleByAtMostTheMaxStep() {
        TurretAim aim = new TurretAim(MAX_TURN);

        aim.tick(Math.PI / 2); // far away in one step, unambiguous shorter direction

        assertThat(aim.radiansAt(1.0)).isCloseTo(MAX_TURN, within(1e-9));
    }

    @Test
    void repeatedTicksEventuallyReachAndHoldTheDesiredAngle() {
        TurretAim aim = new TurretAim(MAX_TURN);
        double target = 1.0;

        for (int i = 0; i < 100; i++) {
            aim.tick(target);
        }

        assertThat(aim.radiansAt(1.0)).isCloseTo(target, within(1e-9));

        aim.tick(target);
        assertThat(aim.radiansAt(1.0)).isCloseTo(target, within(1e-9));
    }

    @Test
    void turnsTheShorterWayAroundWhenTheDesiredAngleIsJustPastTheWrapBoundary() {
        TurretAim aim = new TurretAim(MAX_TURN);
        double justUnderPi = Math.PI - 0.05;
        for (int i = 0; i < 50; i++) {
            aim.tick(justUnderPi);
        }
        double headingBefore = aim.radiansAt(1.0);

        // Just past -PI is actually very close to justUnderPi going the short way, through the
        // wrap boundary - not a near-full-circle turn the long way around.
        double justOverNegativePi = -Math.PI + 0.05;
        aim.tick(justOverNegativePi);
        double headingAfter = aim.radiansAt(1.0);

        assertThat(Math.abs(headingAfter - headingBefore)).isLessThan(0.15);
    }

    @Test
    void radiansAtInterpolatesBetweenThePreviousAndCurrentTickAtTheGivenAlpha() {
        TurretAim aim = new TurretAim(MAX_TURN);
        aim.tick(MAX_TURN * 10); // first tick: previous=0, current=MAX_TURN (clamped)

        assertThat(aim.radiansAt(0.0)).isCloseTo(0.0, within(1e-9));
        assertThat(aim.radiansAt(1.0)).isCloseTo(MAX_TURN, within(1e-9));
        assertThat(aim.radiansAt(0.5)).isCloseTo(MAX_TURN / 2, within(1e-9));
    }

    @Test
    void skippingTickAfterConvergingLeavesTheHeadingFrozenSoInterpolationIsSteady() {
        TurretAim aim = new TurretAim(MAX_TURN);
        double target = MAX_TURN * 5;
        for (int i = 0; i < 20; i++) {
            aim.tick(target); // converges well before 20 ticks (target / MAX_TURN = 5 ticks)
        }
        double heading = aim.radiansAt(1.0);

        // no further tick() calls - simulates an idle tower holding its last heading

        assertThat(aim.radiansAt(0.0)).isCloseTo(heading, within(1e-9));
        assertThat(aim.radiansAt(1.0)).isCloseTo(heading, within(1e-9));
    }

    @Test
    void rejectsNonPositiveMaxTurnRate() {
        assertThatThrownBy(() -> new TurretAim(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TurretAim(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
