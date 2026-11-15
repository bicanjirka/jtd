package td.balance;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.wave.Point;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Which builds the bench reads off a tower's tree, and how it searches for what a build holds. */
class TowerBenchTest {

    private static final TowerBench.Scenario STREAM = TowerBench.allScenarios().stream()
            .filter(scenario -> scenario.name().equals("stream")).findFirst().orElseThrow();

    @Test
    void everyNodeOfEveryTowerIsWeighedAloneInItsTierAndInTheWholeTree() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            List<TowerBench.Build> builds = TowerBench.buildsOf(type);

            for (UpgradeNode node : TowerFactory.createTower(type, WorldFixtures.newWorld(), 0, 0).upgradeTree().nodes()) {
                for (String role : List.of(TowerBench.LINE, TowerBench.WITHOUT, TowerBench.WITHOUT_AT_TOP)) {
                    assertThat(TowerBench.weighing(builds, role, node.id())).as(role + " " + node.id()).isNotEmpty();
                }
            }
        }
    }

    @Test
    void aBuildIsSetAgainstABuildOfTheSameTowerThatDiffersByTheNodeItWeighs() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            List<TowerBench.Build> builds = TowerBench.buildsOf(type);
            Set<String> labels = builds.stream().map(TowerBench.Build::label).collect(Collectors.toSet());

            for (TowerBench.Build build : builds) {
                if (build.versus().isEmpty()) {
                    continue;
                }
                TowerBench.Build against = builds.stream()
                        .filter(other -> other.label().equals(build.versus())).findFirst().orElseThrow();
                boolean owns = build.role().equals(TowerBench.LINE);
                assertThat(labels).as(build.label()).contains(build.versus());
                assertThat(build.owned().contains(build.node())).as(build.label()).isEqualTo(owns);
                assertThat(against.owned().contains(build.node())).as(build.label()).isEqualTo(!owns);
            }
        }
    }

    @Test
    void theHealthABuildHoldsLiesBetweenOneItHoldsAndOneThatLeaks() {
        TowerBench bench = new TowerBench(1, 0.1, List.of(STREAM), TowerBench.Weights.plain());
        List<TowerBench.Stand> stands = List.of(new TowerBench.Stand(TowerFactory.Type.SNIPER, new Point(6, 0), List.of()));

        double held = bench.threshold(STREAM, stands, 60);

        assertThat(held).isPositive();
        assertThat(TowerBench.holds(STREAM, stands, held / 2, 1L)).isTrue();
        assertThat(TowerBench.holds(STREAM, stands, held * 2, 1L)).isFalse();
    }

    @Test
    void theSearchFindsTheSameHealthFromAGuessFarBelowAndFarAbove() {
        TowerBench bench = new TowerBench(1, 0.05, List.of(STREAM), TowerBench.Weights.plain());
        List<TowerBench.Stand> stands = List.of(new TowerBench.Stand(TowerFactory.Type.SNIPER, new Point(6, 0), List.of()));

        double fromBelow = bench.threshold(STREAM, stands, 1);
        double fromAbove = bench.threshold(STREAM, stands, 5_000);

        assertThat(fromBelow / fromAbove).isBetween(0.8, 1.25);
    }

    @Test
    void aTowerThatLandsNothingHoldsNothing() {
        TowerBench bench = new TowerBench(1, 0.1, List.of(STREAM), TowerBench.Weights.plain());

        double held = bench.threshold(STREAM,
                List.of(new TowerBench.Stand(TowerFactory.Type.AURA, new Point(6, 0), List.of())), 60);

        assertThat(held).isZero();
    }

    @Test
    void aWavesEnemiesDifferInHealthAroundTheHealthSearched() {
        double total = 0;
        for (int slot = 0; slot < STREAM.count(); slot++) {
            total += STREAM.share(slot);
        }

        assertThat(total).isCloseTo(STREAM.count(), within(1e-9));
        assertThat(STREAM.share(0)).isNotEqualTo(STREAM.share(1));
    }
}
