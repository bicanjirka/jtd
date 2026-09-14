package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InWedgeTargetQueryTest {

    private static FakeEnemyMob atBearing(double radiansFromOrigin, double distance) {
        int x = (int) Math.round(Math.cos(radiansFromOrigin) * distance);
        int y = (int) Math.round(Math.sin(radiansFromOrigin) * distance);
        return FakeEnemyMob.at(x, y);
    }

    @Test
    void anEnemyDirectlyAheadIsInsideANarrowWedge() {
        FakeEnemyMob ahead = atBearing(0.0, 100);

        List<EnemyMob> matches = new InWedgeTargetQuery(0, 0, 0.0, 0.2)
                .matching(() -> new EnemyMob[]{ahead});

        assertThat(matches).containsExactly(ahead);
    }

    @Test
    void anEnemyWellInsideTheHalfWidthIsIncluded() {
        FakeEnemyMob nearEdge = atBearing(0.3, 100); // half-width is 0.5

        List<EnemyMob> matches = new InWedgeTargetQuery(0, 0, 0.0, 0.5)
                .matching(() -> new EnemyMob[]{nearEdge});

        assertThat(matches).containsExactly(nearEdge);
    }

    @Test
    void anEnemyWellOutsideTheHalfWidthIsExcluded() {
        FakeEnemyMob outside = atBearing(0.8, 100); // half-width is 0.5

        List<EnemyMob> matches = new InWedgeTargetQuery(0, 0, 0.0, 0.5)
                .matching(() -> new EnemyMob[]{outside});

        assertThat(matches).isEmpty();
    }

    @Test
    void anEnemyDirectlyBehindIsExcludedFromAWedgeFacingForward() {
        FakeEnemyMob behind = atBearing(Math.PI, 100);

        List<EnemyMob> matches = new InWedgeTargetQuery(0, 0, 0.0, 0.5)
                .matching(() -> new EnemyMob[]{behind});

        assertThat(matches).isEmpty();
    }

    @Test
    void theWedgeFollowsItsHeadingRatherThanAlwaysPointingAlongPositiveX() {
        FakeEnemyMob ahead = atBearing(Math.PI / 2, 100); // "ahead" relative to a heading of +90 degrees

        List<EnemyMob> matches = new InWedgeTargetQuery(0, 0, Math.PI / 2, 0.2)
                .matching(() -> new EnemyMob[]{ahead});

        assertThat(matches).containsExactly(ahead);
    }

    @Test
    void deadOrInactiveEnemiesAreExcludedRegardlessOfBearing() {
        FakeEnemyMob dead = atBearing(0.0, 100).invalid();

        List<EnemyMob> matches = new InWedgeTargetQuery(0, 0, 0.0, 0.5)
                .matching(() -> new EnemyMob[]{dead});

        assertThat(matches).isEmpty();
    }

    @Test
    void aNonPositiveHalfWidthIsRejected() {
        assertThatThrownBy(() -> new InWedgeTargetQuery(0, 0, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
