package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RandomSelectorTest {

    @Test
    void theOnlyCandidateIsAlwaysSelected() {
        FakeEnemyMob onlyOne = FakeEnemyMob.at(0, 0);

        Optional<EnemyMob> selected = new RandomSelector().selectFrom(List.of(onlyOne));

        assertThat(selected).contains(onlyOne);
    }

    @Test
    void emptyCandidatesSelectsNothing() {
        Optional<EnemyMob> selected = new RandomSelector().selectFrom(List.of());

        assertThat(selected).isEmpty();
    }

    @Test
    void selectionIsAlwaysOneOfTheCandidates() {
        List<EnemyMob> candidates = List.of(FakeEnemyMob.at(0, 0), FakeEnemyMob.at(1, 1), FakeEnemyMob.at(2, 2));

        for (int i = 0; i < 50; i++) {
            assertThat(new RandomSelector().selectFrom(candidates)).get().isIn(candidates);
        }
    }
}
