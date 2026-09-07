package td.economy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyStateTest {

    @Test
    void startingWithSeedsCreditsAndLivesAtZeroScore() {
        EconomyState state = EconomyState.startingWith(50, 5);

        assertThat(state.credits()).isEqualTo(50);
        assertThat(state.score()).isZero();
        assertThat(state.lives()).isEqualTo(5);
    }

    @Test
    void afterAKillAppliesCreditsAndScoreTogether() {
        EconomyState state = EconomyState.startingWith(50, 5).after(EconomyDelta.kill(7));

        assertThat(state.credits()).isEqualTo(57);
        assertThat(state.score()).isEqualTo(7);
        assertThat(state.lives()).isEqualTo(5);
    }

    @Test
    void afterALeakDeductsScoreAndCostsALife() {
        EconomyState state = EconomyState.startingWith(50, 5).after(EconomyDelta.leak(10));

        assertThat(state.score()).isEqualTo(-10);
        assertThat(state.lives()).isEqualTo(4);
    }

    @Test
    void canAffordIsTrueOnlyAtOrAboveTheAmount() {
        EconomyState state = EconomyState.startingWith(40, 5);

        assertThat(state.canAfford(40)).isTrue();
        assertThat(state.canAfford(41)).isFalse();
    }

    @Test
    void isGameOverOnlyOnceLivesReachesZeroOrBelow() {
        assertThat(EconomyState.startingWith(0, 1).isGameOver()).isFalse();
        assertThat(EconomyState.startingWith(0, 0).isGameOver()).isTrue();
        assertThat(EconomyState.startingWith(0, -1).isGameOver()).isTrue();
    }
}
