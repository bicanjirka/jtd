package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InRangeTargetQueryTest {

    @Test
    void enemiesOutsideRangeAreExcluded() {
        FakeEnemyMob near = FakeEnemyMob.at(0, 0);
        FakeEnemyMob far = FakeEnemyMob.at(1000, 1000);

        List<EnemyMob> matches = InRangeTargetQuery.everyone(0, 0, 50)
                .matching(() -> new EnemyMob[]{near, far});

        assertThat(matches).containsExactly(near);
    }

    @Test
    void hiddenEnemiesAreExcludedWhenOnlyVisibleOnesAreWanted() {
        FakeEnemyMob normal = FakeEnemyMob.at(0, 0);
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(0, 0);

        List<EnemyMob> matches = InRangeTargetQuery.visible(0, 0, 50)
                .matching(() -> new EnemyMob[]{normal, hidden});

        assertThat(matches).containsExactly(normal);
    }

    @Test
    void hiddenEnemiesMatchWhenEveryoneIsWanted() {
        FakeEnemyMob normal = FakeEnemyMob.at(0, 0);
        FakeEnemyMob hidden = FakeEnemyMob.ghostAt(0, 0);

        List<EnemyMob> matches = InRangeTargetQuery.everyone(0, 0, 50)
                .matching(() -> new EnemyMob[]{normal, hidden});

        assertThat(matches).containsExactlyInAnyOrder(normal, hidden);
    }

    @Test
    void deadOrInactiveEnemiesAreExcludedRegardlessOfDistance() {
        FakeEnemyMob dead = FakeEnemyMob.at(0, 0);
        dead.invalidate();

        List<EnemyMob> matches = InRangeTargetQuery.everyone(0, 0, 50)
                .matching(() -> new EnemyMob[]{dead});

        assertThat(matches).isEmpty();
    }
}
