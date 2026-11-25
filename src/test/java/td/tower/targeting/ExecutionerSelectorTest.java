package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionerSelectorTest {

    private static final ExecuteThreshold LINE = new ExecuteThreshold(0.15f, Rank.ELITE);

    @Test
    void anEnemyItsCritLeavesUnderTheLineBeatsOnesWithLessHealthOrMoreProgress() {
        EnemyMob executable = FakeEnemyMob.at(0, 0).withHealth(1000).atHealthFraction(0.5f).withProgression(1);
        EnemyMob sparedElite = FakeEnemyMob.at(0, 0).ranked(Rank.ELITE).withHealth(100).atHealthFraction(0.05f)
                .withProgression(900);
        EnemyMob outOfReach = FakeEnemyMob.at(0, 0).withHealth(5000).atHealthFraction(0.9f).withProgression(800);

        assertThat(new ExecutionerSelector(LINE, 1000 - 100).selectFrom(List.of(sparedElite, outOfReach, executable)))
                .contains(executable);
    }

    @Test
    void withNoneToExecuteTheMostProgressWinsWhateverTheHealth() {
        EnemyMob furthest = FakeEnemyMob.at(0, 0).withHealth(500).atHealthFraction(1f).withProgression(10);
        EnemyMob behind = FakeEnemyMob.at(0, 0).withHealth(300).atHealthFraction(1f).withProgression(5);
        EnemyMob ahead = FakeEnemyMob.at(0, 0).withHealth(900).atHealthFraction(1f).withProgression(8);

        assertThat(new ExecutionerSelector(LINE, 0).selectFrom(List.of(furthest, behind, ahead))).contains(furthest);
    }
}
