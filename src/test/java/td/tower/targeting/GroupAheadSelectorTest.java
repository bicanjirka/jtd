package td.tower.targeting;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class GroupAheadSelectorTest {

    private static final float RADIUS = 40f;

    @ParameterizedTest
    @CsvSource({
        "3, 0.3, 0.8, true",
        "3, 0.3, 0.95, false",
        "2, 0.4, 0.9, false",
        "2, 0.5, 0.4, true",
    })
    void picksTheGroupWhoseSizeTimesHowFarAlongWeighsMost(int groupSize, float groupShare, float lonerShare,
                                                          boolean groupWins) {
        List<FakeEnemyMob> group = IntStream.range(0, groupSize)
                .mapToObj(i -> FakeEnemyMob.at(100 + 10 * i, 100).atPathShare(groupShare)).toList();
        EnemyMob loner = FakeEnemyMob.at(500, 500).atPathShare(lonerShare);
        List<EnemyMob> candidates = new ArrayList<>(group);
        candidates.add(loner);

        EnemyMob selected = new GroupAheadSelector(RADIUS).selectFrom(candidates).orElseThrow();

        assertThat(group.contains(selected)).isEqualTo(groupWins);
    }

    @ParameterizedTest
    @CsvSource({"0.5", "0.9"})
    void amongEqualWeightsPicksTheOneClosestToLeaking(float share) {
        EnemyMob behind = FakeEnemyMob.at(100, 100).atPathShare(share).withProgression(1);
        EnemyMob ahead = FakeEnemyMob.at(110, 100).atPathShare(share).withProgression(5);

        assertThat(new GroupAheadSelector(RADIUS).selectFrom(List.of(behind, ahead))).contains(ahead);
    }
}
