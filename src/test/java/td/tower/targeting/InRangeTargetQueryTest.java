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
                .matching(TestContexts.withEnemies(near, far));

        assertThat(matches).containsExactly(near);
    }

    @Test
    void enemiesOfTheWrongTypeAreExcludedWhenATypeIsSpecified() {
        FakeEnemyMob normal = FakeEnemyMob.at(0, 0);
        FakeEnemyMob flying = FakeEnemyMob.at(0, 0).withType(EnemyMob.type.Flying);

        List<EnemyMob> matches = InRangeTargetQuery.ofType(0, 0, 50, EnemyMob.type.Normal)
                .matching(TestContexts.withEnemies(normal, flying));

        assertThat(matches).containsExactly(normal);
    }

    @Test
    void enemiesOfAnyTypeMatchWhenTypeIsNotRestricted() {
        FakeEnemyMob normal = FakeEnemyMob.at(0, 0);
        FakeEnemyMob flying = FakeEnemyMob.at(0, 0).withType(EnemyMob.type.Flying);

        List<EnemyMob> matches = InRangeTargetQuery.anyType(0, 0, 50)
                .matching(TestContexts.withEnemies(normal, flying));

        assertThat(matches).containsExactlyInAnyOrder(normal, flying);
    }

    @Test
    void deadOrInactiveEnemiesAreExcludedRegardlessOfDistance() {
        FakeEnemyMob dead = FakeEnemyMob.at(0, 0).invalid();

        List<EnemyMob> matches = InRangeTargetQuery.anyType(0, 0, 50)
                .matching(TestContexts.withEnemies(dead));

        assertThat(matches).isEmpty();
    }
}
