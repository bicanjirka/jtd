package td.tower.pulse;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TollTrackerTest {

    private static int ticksUntilEarned(TollTracker tracker, List<EnemyMob> inside, int ticksPerStack) {
        int ticks = 0;
        while (tracker.tick(inside, ticksPerStack).isEmpty()) {
            ticks++;
        }
        return ticks + 1;
    }

    @Test
    void anEnemyEarnsAStackAfterTheTicksPerStackAndThenAgainAfterAsManyMore() {
        TollTracker tracker = new TollTracker();
        List<EnemyMob> inside = List.of(FakeEnemyMob.at(0, 0));

        int first = ticksUntilEarned(tracker, inside, 20);
        int second = ticksUntilEarned(tracker, inside, 20);

        assertThat(first).isEqualTo(20);
        assertThat(second).isEqualTo(20);
    }

    @Test
    void eachEnemyCountsForItself() {
        TollTracker tracker = new TollTracker();
        FakeEnemyMob early = FakeEnemyMob.at(0, 0);
        FakeEnemyMob late = FakeEnemyMob.at(1, 0);
        for (int i = 0; i < 10; i++) {
            tracker.tick(List.of(early), 20);
        }

        List<EnemyMob> earners = List.of();
        for (int i = 0; i < 10; i++) {
            earners = tracker.tick(List.of(early, late), 20);
        }

        assertThat(earners).containsExactly(early);
    }

    @Test
    void anEnemyThatLeavesStartsCountingAgainWhenItReturns() {
        TollTracker tracker = new TollTracker();
        FakeEnemyMob enemy = FakeEnemyMob.at(0, 0);
        for (int i = 0; i < 19; i++) {
            tracker.tick(List.of(enemy), 20);
        }
        tracker.tick(List.of(), 20);

        List<EnemyMob> earners = tracker.tick(List.of(enemy), 20);

        assertThat(earners).isEmpty();
        assertThat(ticksUntilEarned(tracker, List.of(enemy), 20)).isEqualTo(19);
    }
}
