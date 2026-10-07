package td.tower.splash;

import org.junit.jupiter.api.Test;
import td.enemy.EnemyMob;
import td.fixtures.FakeEnemyMob;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArcPlannerTest {

    private static final double REACH = 40;

    private static List<ArcStrike> plan(EnemyMob primary, List<EnemyMob> candidates, ArcSpec spec) {
        return ArcPlanner.plan(primary, primary, candidates, Set.of(primary), spec, from -> REACH);
    }

    @Test
    void eachJumpGoesToTheNearestEnemyNotYetHitWithinReach() {
        FakeEnemyMob primary = FakeEnemyMob.at(0, 0);
        FakeEnemyMob near = FakeEnemyMob.at(20, 0);
        FakeEnemyMob next = FakeEnemyMob.at(50, 0);
        FakeEnemyMob decoy = FakeEnemyMob.at(0, 35);
        FakeEnemyMob outOfReach = FakeEnemyMob.at(200, 0);

        List<ArcStrike> strikes = plan(primary, List.of(primary, near, next, decoy, outOfReach), ArcSpec.arcs());

        assertThat(strikes).extracting(ArcStrike::to).containsExactly(near, next);
        assertThat(strikes).extracting(ArcStrike::share).containsOnly(ArcSpec.BASE_SHARE);
    }

    @Test
    void theArcsStopWhenNothingIsLeftInReach() {
        FakeEnemyMob primary = FakeEnemyMob.at(0, 0);
        FakeEnemyMob only = FakeEnemyMob.at(20, 0);

        List<ArcStrike> strikes = plan(primary, List.of(primary, only), ArcSpec.arcs());

        assertThat(strikes).extracting(ArcStrike::to).containsExactly(only);
    }

    @Test
    void aForkingSpecSplitsTheThirdJumpIntoTwoArcsThatEachRunOn() {
        FakeEnemyMob primary = FakeEnemyMob.at(0, 0);
        FakeEnemyMob a = FakeEnemyMob.at(30, 0);
        FakeEnemyMob b = FakeEnemyMob.at(60, 0);
        FakeEnemyMob forkUp = FakeEnemyMob.at(80, -20);
        FakeEnemyMob forkDown = FakeEnemyMob.at(80, 20);
        FakeEnemyMob beyondUp = FakeEnemyMob.at(100, -50);
        FakeEnemyMob beyondDown = FakeEnemyMob.at(100, 50);
        ArcSpec spec = ArcSpec.arcs().withMinimumJumps(4).thatForks();

        List<ArcStrike> strikes = plan(primary, List.of(primary, a, b, forkUp, forkDown, beyondUp, beyondDown), spec);

        assertThat(strikes).extracting(ArcStrike::to)
                .containsExactlyInAnyOrder(a, b, forkUp, forkDown, beyondUp, beyondDown);
    }

    @Test
    void anArcWithNowhereToGoReturnsToThePrimaryAtFullStrengthUpToItsReturns() {
        FakeEnemyMob primary = FakeEnemyMob.at(0, 0);
        ArcSpec spec = ArcSpec.arcs().withMinimumJumps(5).withReturns(3);

        List<ArcStrike> strikes = plan(primary, List.of(primary), spec);

        assertThat(strikes).hasSize(3).allSatisfy(strike -> {
            assertThat(strike.to()).isSameAs(primary);
            assertThat(strike.share()).isEqualTo(1f);
        });
    }

    @Test
    void jumpsAddUpAndAMinimumOnlyRaisesThem() {
        assertThat(ArcSpec.arcs().withMoreJumps(2).jumps()).isEqualTo(4);
        assertThat(ArcSpec.arcs().withMoreJumps(2).withMinimumJumps(6).jumps()).isEqualTo(6);
        assertThat(ArcSpec.arcs().withMinimumJumps(6).withMoreJumps(5).jumps()).isEqualTo(7);
    }
}
