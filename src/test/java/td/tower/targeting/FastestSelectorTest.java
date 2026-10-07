package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FastestSelectorTest {

    @Test
    void theCandidateMovingFastestIsSelected() {
        FakeEnemyMob walker = FakeEnemyMob.at(0, 0).movingAt(1f).withProgression(90);
        FakeEnemyMob runner = FakeEnemyMob.at(0, 0).movingAt(3f).withProgression(10);

        Optional<EnemyMob> selected = new FastestSelector().selectFrom(List.of(walker, runner));

        assertThat(selected).contains(runner);
    }

    @Test
    void aStoppedEnemyLosesTheLeadToAnyEnemyThatMoves() {
        FakeEnemyMob stopped = FakeEnemyMob.at(0, 0).movingAt(0f).withProgression(90);
        FakeEnemyMob crawler = FakeEnemyMob.at(0, 0).movingAt(0.1f).withProgression(10);

        Optional<EnemyMob> selected = new FastestSelector().selectFrom(List.of(stopped, crawler));

        assertThat(selected).contains(crawler);
    }

    @Test
    void equallyFastCandidatesTieOnWhoIsFurthestAlongThePath() {
        FakeEnemyMob behind = FakeEnemyMob.at(0, 0).movingAt(2f).withProgression(10);
        FakeEnemyMob ahead = FakeEnemyMob.at(0, 0).movingAt(2f).withProgression(90);

        Optional<EnemyMob> selected = new FastestSelector().selectFrom(List.of(behind, ahead));

        assertThat(selected).contains(ahead);
    }

    @Test
    void emptyCandidatesSelectsNothing() {
        assertThat(new FastestSelector().selectFrom(List.of())).isEmpty();
    }
}
