package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HighestRankSelectorTest {

    @Test
    void picksTheHighestRankAmongTheCandidates() {
        EnemyMob grunt = FakeEnemyMob.at(0, 0).withProgression(900);
        EnemyMob boss = FakeEnemyMob.at(0, 0).ranked(Rank.BOSS).withProgression(1);
        EnemyMob elite = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withProgression(500);

        assertThat(new HighestRankSelector().selectFrom(List.of(grunt, boss, elite))).contains(boss);
    }

    @Test
    void amongEqualRanksPicksTheOneClosestToLeaking() {
        EnemyMob behind = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withProgression(10);
        EnemyMob ahead = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withProgression(20);

        assertThat(new HighestRankSelector().selectFrom(List.of(behind, ahead))).contains(ahead);
    }

    @Test
    void selectingFromNoCandidatesReturnsEmpty() {
        assertThat(new HighestRankSelector().selectFrom(List.of())).isEmpty();
    }
}
