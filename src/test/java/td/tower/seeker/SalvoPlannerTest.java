package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.tower.targeting.FastestSelector;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SalvoPlannerTest {

    private final FastestSelector fastest = new FastestSelector();

    @Test
    void withNothingAvoidedItIsTheSelectorsPick() {
        FakeEnemyMob slow = FakeEnemyMob.at(0, 0).movingAt(1f);
        FakeEnemyMob quick = FakeEnemyMob.at(0, 0).movingAt(3f);

        Optional<EnemyMob> pick = SalvoPlanner.pick(this.fastest, List.of(slow, quick), Set.of());

        assertThat(pick).contains(quick);
    }

    @Test
    void anEnemyAlreadyFiredAtGivesWayToTheNextOne() {
        FakeEnemyMob slow = FakeEnemyMob.at(0, 0).movingAt(1f);
        FakeEnemyMob quick = FakeEnemyMob.at(0, 0).movingAt(3f);

        Optional<EnemyMob> pick = SalvoPlanner.pick(this.fastest, List.of(slow, quick), Set.of(quick));

        assertThat(pick).contains(slow);
    }

    @Test
    void whenEveryEnemyHasBeenFiredAtItIsTheSelectorsPickAmongAllOfThem() {
        FakeEnemyMob slow = FakeEnemyMob.at(0, 0).movingAt(1f);
        FakeEnemyMob quick = FakeEnemyMob.at(0, 0).movingAt(3f);

        Optional<EnemyMob> pick = SalvoPlanner.pick(this.fastest, List.of(slow, quick), Set.of(slow, quick));

        assertThat(pick).contains(quick);
    }

    @Test
    void withNoCandidatesThereIsNoPick() {
        assertThat(SalvoPlanner.pick(this.fastest, List.of(), Set.of())).isEmpty();
    }
}
