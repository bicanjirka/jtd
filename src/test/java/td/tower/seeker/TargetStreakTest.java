package td.tower.seeker;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeEnemyMob;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetStreakTest {

    private final TargetStreak streak = new TargetStreak();

    @Test
    void eachConsecutiveImpactOnTheSameEnemyAddsOneToTheStreak() {
        FakeEnemyMob target = FakeEnemyMob.at(0, 0);
        List<Integer> before = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            before.add(this.streak.on(target));
            this.streak.landed(target);
        }

        assertThat(before).containsExactly(0, 1, 2, 3);
    }

    @Test
    void anImpactOnAnotherEnemyStartsTheStreakOver() {
        FakeEnemyMob first = FakeEnemyMob.at(0, 0);
        FakeEnemyMob second = FakeEnemyMob.at(10, 0);
        this.streak.landed(first);
        this.streak.landed(first);

        int other = this.streak.on(second);
        this.streak.landed(second);

        assertThat(other).isZero();
        assertThat(this.streak.on(first)).isZero();
        assertThat(this.streak.on(second)).isEqualTo(1);
    }
}
