package td.tower.sonar;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;
import td.tower.targeting.Viewpoint;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PingTrackerTest {

    /** Enough alike that a hash order could not pass for the order they came in. */
    private static final int ALIKE = 64;

    @Test
    void enemiesAlikeInHealthAndProgressArePickedInTheOrderTheBeamPassedThem() {
        PingTracker tracker = new PingTracker();
        List<EnemyMob> passed = Stream.generate(() -> (EnemyMob) FakeEnemyMob.at(10, 0)).limit(ALIKE).toList();
        passed.forEach(tracker::pass);

        List<EnemyMob> picked = tracker.pick(PingRule.none().withCount(3), new Viewpoint(0, 0, 100f, 32));

        assertThat(picked).containsExactlyElementsOf(passed.subList(0, 3));
    }
}
