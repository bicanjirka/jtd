package td;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoundStateTest {

    private static final int WAVES = 3;

    @Test
    void aBoardWaitingForItsNextWaveStopsBeingReadyOnceAnEnemyJoinsIt() {
        RoundState ready = RoundState.before(1);

        RoundState joined = ready.joinedBy(1);

        assertThat(joined.isWaveReady()).isFalse();
        assertThat(joined.canBegin(WAVES)).isFalse();
    }

    @Test
    void aBoardHeldByEnemiesNoWaveSentIsReadyAgainOnceTheyAreGone() {
        RoundState held = RoundState.before(1).joinedBy(2);

        RoundState cleared = held.settled(0, 0L, false, WAVES);

        assertThat(cleared.isWaveReady()).isTrue();
        assertThat(cleared.wave()).isEqualTo(1);
    }

    @Test
    void anEnemyLeakingWhileNoWaveRunsMarksNoRoundAsLeaked() {
        RoundState held = RoundState.before(1).begun(0L).settled(0, 0L, false, WAVES).joinedBy(1);

        RoundState cleared = held.settled(0, 1L, false, WAVES);

        assertThat(cleared.leaked()).isEmpty();
    }

    @Test
    void anEnemyJoiningARunningWaveLeavesItRunning() {
        RoundState running = RoundState.before(0).begun(0L);

        RoundState joined = running.joinedBy(5);

        assertThat(joined).isEqualTo(running);
    }
}
