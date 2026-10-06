package td.tower.sniper;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeEnemyMob;

import static org.assertj.core.api.Assertions.assertThat;

class AimFocusTest {

    private final AimFocus focus = new AimFocus();

    @Test
    void theFirstShotAtATargetIsFreshAndHasNoStacks() {
        AimLock lock = this.focus.lock(FakeEnemyMob.at(0, 0), 3, false);

        assertThat(lock).isEqualTo(new AimLock(0, true));
    }

    @Test
    void everyShotAtTheSameTargetAddsAStackUpToTheCap() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        this.focus.lock(target, 2, false);

        AimLock second = this.focus.lock(target, 2, false);
        AimLock third = this.focus.lock(target, 2, false);
        AimLock fourth = this.focus.lock(target, 2, false);

        assertThat(second).isEqualTo(new AimLock(1, false));
        assertThat(third).isEqualTo(new AimLock(2, false));
        assertThat(fourth).isEqualTo(new AimLock(2, false));
    }

    @Test
    void aimingAtAnotherLivingEnemyStartsOver() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        this.focus.lock(first, 3, true);
        this.focus.lock(first, 3, true);

        AimLock other = this.focus.lock(FakeEnemyMob.at(5, 5), 3, true);

        assertThat(other).isEqualTo(new AimLock(0, true));
    }

    @Test
    void theStacksSurviveAKillOnlyWhenAskedTo() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        this.focus.lock(first, 3, true);
        this.focus.lock(first, 3, true);
        first.invalidate();

        AimLock carried = this.focus.lock(FakeEnemyMob.at(5, 5), 3, true);

        assertThat(carried).isEqualTo(new AimLock(2, true));
    }

    @Test
    void withoutSurvivingAKillTheStacksEndWithTheTarget() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        this.focus.lock(first, 3, false);
        this.focus.lock(first, 3, false);
        first.invalidate();

        AimLock lost = this.focus.lock(FakeEnemyMob.at(5, 5), 3, false);

        assertThat(lost).isEqualTo(new AimLock(0, true));
    }
}
