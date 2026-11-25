package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HuntedFirstSelectorTest {

    @Test
    void anEliteOrBossBeatsAGruntFurtherAlongAndAmongThemTheFurthestWins() {
        EnemyMob grunt = FakeEnemyMob.at(0, 0).withProgression(900);
        EnemyMob boss = FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).withProgression(100);
        EnemyMob elite = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withProgression(300);

        assertThat(new HuntedFirstSelector().selectFrom(List.of(grunt, boss, elite))).contains(elite);
    }

    @Test
    void withNoneHuntedTheFurthestAlongWins() {
        EnemyMob behind = FakeEnemyMob.at(0, 0).withProgression(10);
        EnemyMob ahead = FakeEnemyMob.at(0, 0).withProgression(20);

        assertThat(new HuntedFirstSelector().selectFrom(List.of(behind, ahead))).contains(ahead);
    }
}
