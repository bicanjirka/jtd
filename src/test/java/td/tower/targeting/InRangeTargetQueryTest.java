package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InRangeTargetQueryTest {

    @Test
    void enemiesOutsideRangeAreExcluded() {
        FakeEnemyMob near = FakeEnemyMob.at(0, 0);
        FakeEnemyMob far = FakeEnemyMob.at(1000, 1000);

        List<EnemyMob> matches = InRangeTargetQuery.anyType(0, 0, 50)
                .matching(() -> new EnemyMob[]{near, far});

        assertThat(matches).containsExactly(near);
    }

    @Test
    void enemiesOfTheWrongTypeAreExcludedWhenATypeIsSpecified() {
        FakeEnemyMob normal = FakeEnemyMob.at(0, 0);
        FakeEnemyMob flying = FakeEnemyMob.at(0, 0).withType(EnemyMob.Type.FLYING);

        List<EnemyMob> matches = InRangeTargetQuery.ofType(0, 0, 50, EnemyMob.Type.NORMAL)
                .matching(() -> new EnemyMob[]{normal, flying});

        assertThat(matches).containsExactly(normal);
    }

    @Test
    void enemiesOfAnyTypeMatchWhenTypeIsNotRestricted() {
        FakeEnemyMob normal = FakeEnemyMob.at(0, 0);
        FakeEnemyMob flying = FakeEnemyMob.at(0, 0).withType(EnemyMob.Type.FLYING);

        List<EnemyMob> matches = InRangeTargetQuery.anyType(0, 0, 50)
                .matching(() -> new EnemyMob[]{normal, flying});

        assertThat(matches).containsExactlyInAnyOrder(normal, flying);
    }

    @Test
    void deadOrInactiveEnemiesAreExcludedRegardlessOfDistance() {
        FakeEnemyMob dead = FakeEnemyMob.at(0, 0).invalid();

        List<EnemyMob> matches = InRangeTargetQuery.anyType(0, 0, 50)
                .matching(() -> new EnemyMob[]{dead});

        assertThat(matches).isEmpty();
    }
}
