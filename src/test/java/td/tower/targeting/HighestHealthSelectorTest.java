package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HighestHealthSelectorTest {

    @Test
    void picksTheCandidateWithTheMostHealthRemaining() {
        EnemyMob weak = FakeEnemyMob.at(0, 0).withHealth(10);
        EnemyMob strong = FakeEnemyMob.at(0, 0).withHealth(9000);

        Optional<EnemyMob> selected = new HighestHealthSelector().selectFrom(List.of(weak, strong));

        assertThat(selected).contains(strong);
    }

    @Test
    void selectingFromNoCandidatesReturnsEmpty() {
        Optional<EnemyMob> selected = new HighestHealthSelector().selectFrom(List.of());

        assertThat(selected).isEmpty();
    }
}
