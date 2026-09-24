package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NearestSelectorTest {

    @Test
    void theClosestCandidateToTheGivenPointIsSelected() {
        FakeEnemyMob near = FakeEnemyMob.at(5, 0);
        FakeEnemyMob far = FakeEnemyMob.at(50, 0);

        Optional<EnemyMob> selected = new NearestSelector(0, 0).selectFrom(List.of(far, near));

        assertThat(selected).contains(near);
    }

    @Test
    void emptyCandidatesSelectsNothing() {
        Optional<EnemyMob> selected = new NearestSelector(0, 0).selectFrom(List.of());

        assertThat(selected).isEmpty();
    }
}
