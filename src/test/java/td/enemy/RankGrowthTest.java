package td.enemy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RankGrowthTest {

    // A Boss at 17 times a grunt puts the ranks between at 2, 5 and 10 times: one more sixteenth of the gap for
    // each square of the rank's place.
    private static final RankGrowth GROWTH = RankGrowth.of(17f);

    private static RankedEnemy.Builder ladder() {
        return RankedEnemy.startingAt(EnemyDefinition.of("x", "X", 100, 4, 1f, BodyArchetype.CIRCLE), GROWTH);
    }

    @Test
    void healthBendsUpwardWhileBountyFollowsItsSquareRoot() {
        RankedEnemy ladder = ladder().build();

        assertThat(Arrays.stream(Rank.values()).map(rank -> ladder.definitionFor(rank).baseHealth()))
                .containsExactly(100, 200, 500, 1000, 1700);
        assertThat(Arrays.stream(Rank.values()).map(rank -> ladder.definitionFor(rank).price()))
                .containsExactly(4, (int) Math.round(4 * Math.sqrt(2)), (int) Math.round(4 * Math.sqrt(5)),
                        (int) Math.round(4 * Math.sqrt(10)), (int) Math.round(4 * Math.sqrt(17)));
    }

    @Test
    void aGrowingLadderGrowsTheRanksItSkipsAndKeepsWhatEachStepAdds() {
        RankedEnemy ladder = ladder().thenAt(Rank.VETERAN, e -> e.withDescription("veteran")).build();

        assertThat(ladder.definitionFor(Rank.SOLDIER).baseHealth()).isEqualTo(200);
        assertThat(ladder.definitionFor(Rank.SOLDIER).description()).isEmpty();
        assertThat(ladder.definitionFor(Rank.VETERAN).baseHealth()).isEqualTo(500);
        assertThat(ladder.effectiveRank(Rank.BOSS)).isEqualTo(Rank.BOSS);
        assertThat(ladder.definitionFor(Rank.BOSS).description()).isEqualTo("veteran");
        assertThat(ladder.definitionFor(Rank.BOSS).baseHealth()).isEqualTo(1700);
    }

    @Test
    void aStepsOwnHealthAndBountyOverrideTheGrowthForThatRankAlone() {
        RankedEnemy ladder = ladder().thenAt(Rank.VETERAN, e -> e.withHealthAndPrice(999, 50)).build();

        assertThat(ladder.definitionFor(Rank.VETERAN).baseHealth()).isEqualTo(999);
        assertThat(ladder.definitionFor(Rank.VETERAN).price()).isEqualTo(50);
        assertThat(ladder.definitionFor(Rank.ELITE).baseHealth()).isEqualTo(1000);
    }

    @Test
    void aBossNoTougherThanItsGruntIsRejected() {
        assertThatThrownBy(() -> RankGrowth.of(1f)).isInstanceOf(IllegalArgumentException.class);
    }
}
