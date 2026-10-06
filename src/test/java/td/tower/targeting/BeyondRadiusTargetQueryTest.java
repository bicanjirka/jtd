package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;

class BeyondRadiusTargetQueryTest {

    @Test
    void matchesOnlyEnemiesAtLeastTheRadiusAway() {
        FakeEnemyMob near = FakeEnemyMob.at(30, 0);
        FakeEnemyMob far = FakeEnemyMob.at(80, 0);

        var matches = new BeyondRadiusTargetQuery(0, 0, 64f).matching(() -> new EnemyMob[]{near, far});

        assertThat(matches).containsExactly(far);
    }

    @Test
    void ignoresEnemiesThatAreNotOnTheBoard() {
        FakeEnemyMob gone = FakeEnemyMob.at(200, 0);
        gone.invalidate();

        assertThat(new BeyondRadiusTargetQuery(0, 0, 64f).matching(() -> new EnemyMob[]{gone})).isEmpty();
    }
}
