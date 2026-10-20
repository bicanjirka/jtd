package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeEnemyMob;
import td.util.TickRate;

import static org.assertj.core.api.Assertions.assertThat;

class AimFocusTest {

    private static final int STACK_TICKS = Math.round(AimFocus.STACK_SECONDS * TickRate.TICKS_PER_SECOND);

    private final AimFocus focus = new AimFocus();

    private void lockTimes(int shots) {
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        for (int i = 0; i < shots; i++) {
            this.focus.lock(target, 5);
        }
    }

    private void advance(int ticks) {
        for (int i = 0; i < ticks; i++) {
            this.focus.advance();
        }
    }

    @Test
    void theFirstShotIsFreshAndHasNoStacks() {
        AimLock lock = this.focus.lock(FakeEnemyMob.at(0, 0), 3);

        assertThat(lock).isEqualTo(new AimLock(0, true));
    }

    @Test
    void everyShotAddsAStackUpToTheCap() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        this.focus.lock(target, 2);

        AimLock second = this.focus.lock(target, 2);
        AimLock third = this.focus.lock(target, 2);
        AimLock fourth = this.focus.lock(target, 2);

        assertThat(second).isEqualTo(new AimLock(1, false));
        assertThat(third).isEqualTo(new AimLock(2, false));
        assertThat(fourth).isEqualTo(new AimLock(2, false));
    }

    @Test
    void aimingAtAnotherLivingEnemyIsFreshButKeepsTheStacks() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        this.focus.lock(first, 3);
        this.focus.lock(first, 3);

        AimLock other = this.focus.lock(FakeEnemyMob.at(5, 5), 3);

        assertThat(other).isEqualTo(new AimLock(2, true));
    }

    @Test
    void theStacksOutliveAKilledTarget() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        this.focus.lock(first, 3);
        this.focus.lock(first, 3);
        first.invalidate();

        AimLock next = this.focus.lock(FakeEnemyMob.at(5, 5), 3);

        assertThat(next).isEqualTo(new AimLock(2, true));
    }

    @Test
    void theStacksTheNextShotHasAreTheOnesBuiltSoFar() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        this.focus.lock(target, 3);
        this.focus.lock(target, 3);

        int stacks = this.focus.stacks();

        assertThat(stacks).isEqualTo(2);
    }

    @Test
    void aStackFadesThreeSecondsAfterTheLastShotOneAtATime() {
        this.lockTimes(3);

        this.advance(STACK_TICKS - 1);
        int beforeTheFade = this.focus.stacks();
        this.advance(1);
        int afterOne = this.focus.stacks();
        this.advance(STACK_TICKS);
        int afterTwo = this.focus.stacks();

        assertThat(beforeTheFade).isEqualTo(3);
        assertThat(afterOne).isEqualTo(2);
        assertThat(afterTwo).isEqualTo(1);
    }

    @Test
    void aShotStartsTheFadeOver() {
        this.lockTimes(2);
        this.advance(STACK_TICKS - 1);

        this.lockTimes(1);
        this.advance(STACK_TICKS - 1);

        assertThat(this.focus.stacks()).isEqualTo(3);
    }

    @Test
    void theStacksFadeToNoneAndStayThere() {
        this.lockTimes(2);

        this.advance(5 * STACK_TICKS);

        assertThat(this.focus.stacks()).isZero();
    }
}
