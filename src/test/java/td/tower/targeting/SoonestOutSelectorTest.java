package td.tower.targeting;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SoonestOutSelectorTest {

    @Test
    void aFastEnemyFarBackIsPickedOverASlowOneNearTheExitWhenItWouldLeaveSooner() {
        FakeEnemyMob slowNear = FakeEnemyMob.at(0, 0).withDistanceToExit(100).movingAt(1f);
        FakeEnemyMob fastFar = FakeEnemyMob.at(0, 0).withDistanceToExit(300).movingAt(4f);

        assertThat(new SoonestOutSelector().selectFrom(List.<EnemyMob>of(slowNear, fastFar))).contains(fastFar);
    }

    @Test
    void anEnemyThatStandsStillComesLastHoweverNearTheExit() {
        FakeEnemyMob stopped = FakeEnemyMob.at(0, 0).withDistanceToExit(1).movingAt(0f);
        FakeEnemyMob walking = FakeEnemyMob.at(0, 0).withDistanceToExit(900).movingAt(1f);

        assertThat(new SoonestOutSelector().selectFrom(List.<EnemyMob>of(stopped, walking))).contains(walking);
    }

    @Test
    void ofTwoThatWouldLeaveTogetherTheOneNearerTheExitIsPicked() {
        FakeEnemyMob near = FakeEnemyMob.at(0, 0).withDistanceToExit(10).movingAt(0f);
        FakeEnemyMob far = FakeEnemyMob.at(0, 0).withDistanceToExit(20).movingAt(0f);

        assertThat(new SoonestOutSelector().selectFrom(List.<EnemyMob>of(far, near))).contains(near);
    }
}
