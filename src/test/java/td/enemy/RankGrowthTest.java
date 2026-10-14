package td.enemy;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RankGrowthTest {

    @Test
    void healthMultipliesEachRankWhileBountyAddsStepsThatGrowForEliteAndBoss() {
        RankedEnemy ladder = RankedEnemy
                .startingAt(EnemyDefinition.of("x", "X", 100, 3, 1f, BodyArchetype.CIRCLE), RankGrowth.of(2f, 4))
                .build();

        assertThat(Arrays.stream(Rank.values()).map(rank -> ladder.definitionFor(rank).baseHealth()))
                .containsExactly(100, 200, 400, 800, 1600);
        assertThat(Arrays.stream(Rank.values()).map(rank -> ladder.definitionFor(rank).price()))
                .containsExactly(3, 7, 11, 23, 43);
    }

    @Test
    void aGrowingLadderGrowsTheRanksItSkipsAndKeepsWhatEachStepAdds() {
        RankedEnemy ladder = RankedEnemy
                .startingAt(EnemyDefinition.of("x", "X", 100, 3, 1f, BodyArchetype.CIRCLE), RankGrowth.of(1.5f, 1))
                .thenAt(Rank.VETERAN, e -> e.withDescription("veteran"))
                .build();

        assertThat(ladder.definitionFor(Rank.SOLDIER).baseHealth()).isEqualTo(150);
        assertThat(ladder.definitionFor(Rank.SOLDIER).description()).isEmpty();
        assertThat(ladder.definitionFor(Rank.VETERAN).baseHealth()).isEqualTo(225);
        assertThat(ladder.effectiveRank(Rank.BOSS)).isEqualTo(Rank.BOSS);
        assertThat(ladder.definitionFor(Rank.BOSS).description()).isEqualTo("veteran");
        assertThat(ladder.definitionFor(Rank.BOSS).price()).isEqualTo(13);
    }
}
