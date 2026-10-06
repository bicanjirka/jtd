package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;

class OnSegmentTargetQueryTest {

    private static OnSegmentTargetQuery alongTheXAxis() {
        return new OnSegmentTargetQuery(0, 0, 200, 0, 10);
    }

    @Test
    void matchesEnemiesOnTheLineNearestTheStartFirst() {
        FakeEnemyMob far = FakeEnemyMob.at(150, 4);
        FakeEnemyMob near = FakeEnemyMob.at(40, -3);

        var matches = alongTheXAxis().matching(() -> new EnemyMob[]{far, near});

        assertThat(matches).containsExactly(near, far);
    }

    @Test
    void skipsEnemiesBesideTheLineBehindTheStartOrPastTheEnd() {
        FakeEnemyMob beside = FakeEnemyMob.at(100, 30);
        FakeEnemyMob behind = FakeEnemyMob.at(-30, 0);
        FakeEnemyMob past = FakeEnemyMob.at(260, 0);

        assertThat(alongTheXAxis().matching(() -> new EnemyMob[]{beside, behind, past})).isEmpty();
    }

    @Test
    void skipsAHiddenEnemyOnTheLine() {
        FakeEnemyMob ghost = FakeEnemyMob.ghostAt(100, 0);

        assertThat(alongTheXAxis().matching(() -> new EnemyMob[]{ghost})).isEmpty();
    }
}
