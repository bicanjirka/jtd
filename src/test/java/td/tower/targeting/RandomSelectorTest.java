package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.util.RandomSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RandomSelectorTest {

    private static List<EnemyMob> picksFrom(List<EnemyMob> candidates, RandomSource source) {
        RandomSelector selector = new RandomSelector(source);
        List<EnemyMob> picks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            selector.selectFrom(candidates).ifPresent(picks::add);
        }
        return picks;
    }

    @Test
    void theOnlyCandidateIsAlwaysSelected() {
        FakeEnemyMob onlyOne = FakeEnemyMob.at(0, 0);

        Optional<EnemyMob> selected = new RandomSelector(RandomSource.shared()).selectFrom(List.of(onlyOne));

        assertThat(selected).contains(onlyOne);
    }

    @Test
    void emptyCandidatesSelectsNothing() {
        Optional<EnemyMob> selected = new RandomSelector(RandomSource.shared()).selectFrom(List.of());

        assertThat(selected).isEmpty();
    }

    @Test
    void aSeededSourceReplaysTheSameSelections() {
        List<EnemyMob> candidates = List.of(FakeEnemyMob.at(0, 0), FakeEnemyMob.at(1, 1), FakeEnemyMob.at(2, 2));

        List<EnemyMob> first = picksFrom(candidates, RandomSource.seeded(42L));
        List<EnemyMob> second = picksFrom(candidates, RandomSource.seeded(42L));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void theSourceDecidesTheSelection() {
        List<EnemyMob> candidates = List.of(FakeEnemyMob.at(0, 0), FakeEnemyMob.at(1, 1), FakeEnemyMob.at(2, 2));

        Optional<EnemyMob> selected = new RandomSelector(() -> 0.9).selectFrom(candidates);

        assertThat(selected).contains(candidates.get(2));
    }

    @Test
    void selectionIsAlwaysOneOfTheCandidates() {
        List<EnemyMob> candidates = List.of(FakeEnemyMob.at(0, 0), FakeEnemyMob.at(1, 1), FakeEnemyMob.at(2, 2));

        for (int i = 0; i < 50; i++) {
            assertThat(new RandomSelector(RandomSource.shared()).selectFrom(candidates)).get().isIn(candidates);
        }
    }
}
