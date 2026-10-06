package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MostNeighboursSelectorTest {

    @Test
    void picksTheCandidateWithTheMostOthersWithinTheRadius() {
        EnemyMob loner = FakeEnemyMob.at(500, 500).withProgression(900);
        EnemyMob hub = FakeEnemyMob.at(100, 100);
        EnemyMob nearOne = FakeEnemyMob.at(120, 100);
        EnemyMob nearTwo = FakeEnemyMob.at(100, 130);

        var selected = new MostNeighboursSelector(40f).selectFrom(List.of(loner, hub, nearOne, nearTwo));

        assertThat(selected).contains(hub);
    }

    @Test
    void amongEqualCountsPicksTheOneClosestToLeaking() {
        EnemyMob behind = FakeEnemyMob.at(100, 100).withProgression(1);
        EnemyMob ahead = FakeEnemyMob.at(110, 100).withProgression(5);

        assertThat(new MostNeighboursSelector(40f).selectFrom(List.of(behind, ahead))).contains(ahead);
    }
}
