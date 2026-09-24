package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FurthestAlongPathSelectorTest {

    @Test
    void theCandidateWithTheHighestProgressionIsSelected() {
        FakeEnemyMob behind = FakeEnemyMob.at(0, 0).withProgression(10);
        FakeEnemyMob ahead = FakeEnemyMob.at(0, 0).withProgression(90);

        Optional<EnemyMob> selected = new FurthestAlongPathSelector().selectFrom(List.of(behind, ahead));

        assertThat(selected).contains(ahead);
    }

    @Test
    void theEarlierCandidateWinsATieOnProgression() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0).withProgression(50);
        FakeEnemyMob second = FakeEnemyMob.at(1, 1).withProgression(50);

        Optional<EnemyMob> selected = new FurthestAlongPathSelector().selectFrom(List.of(first, second));

        assertThat(selected).contains(first);
    }

    @Test
    void emptyCandidatesSelectsNothing() {
        Optional<EnemyMob> selected = new FurthestAlongPathSelector().selectFrom(List.of());

        assertThat(selected).isEmpty();
    }
}
