package td.projectile;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CannonballProjectileTest {

    @Test
    void travelsInAStraightLineTowardItsFixedDestination() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        });

        shell.doTick(1);

        assertThat(shell.getX()).isCloseTo(10.0, within(1e-9));
        assertThat(shell.getY()).isEqualTo(0.0);
        assertThat(shell.isFinished()).isFalse();
    }

    @Test
    void detonatesExactlyAtItsDestinationAndReportsThePointItArrivedAt() {
        List<double[]> impacts = new ArrayList<>();
        CannonballProjectile shell = new CannonballProjectile(0, 0, 30, 0, 10f, (x, y) -> impacts.add(new double[]{x, y}));

        for (int t = 1; t <= 3 && !shell.isFinished(); t++) {
            shell.doTick(t);
        }

        assertThat(shell.isFinished()).isTrue();
        assertThat(shell.getX()).isCloseTo(30.0, within(1e-9));
        assertThat(impacts).hasSize(1);
        assertThat(impacts.getFirst()).containsExactly(30.0, 0.0);
    }

    @Test
    void aFinishedProjectileDoesNothingOnFurtherTicks() {
        List<double[]> impacts = new ArrayList<>();
        CannonballProjectile shell = new CannonballProjectile(0, 0, 10, 0, 10f, (x, y) -> impacts.add(new double[]{x, y}));

        shell.doTick(1); // arrives in exactly one tick (distance 10 == speed 10)
        shell.doTick(2);
        shell.doTick(3);

        assertThat(impacts).hasSize(1);
    }

    @Test
    void interpolationSourceTracksOneTickBehindCurrentPosition() {
        CannonballProjectile shell = new CannonballProjectile(0, 0, 100, 0, 10f, (x, y) -> {
        });

        shell.doTick(1);
        double afterFirstTickX = shell.getX();
        shell.doTick(2);

        assertThat(shell.getPrevX()).isEqualTo(afterFirstTickX);
    }
}
