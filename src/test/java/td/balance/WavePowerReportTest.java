package td.balance;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import td.wave.Point;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class WavePowerReportTest {

    private static final Offset<Double> EXACT = Offset.offset(1e-9);
    private static final double GROWTH = 2;
    private static final WavePowerReport.Yardstick TWO_STEPS = new WavePowerReport.Yardstick(
            List.of(new WavePowerReport.Yardstick.Step(10, 4), new WavePowerReport.Yardstick.Step(40, 10)), 2, GROWTH);

    @Test
    void theYardstickIsTheAverageOfTheTowersAtEachStage() {
        TowerSheet sheet = TowerSheetTest.sheet(List.of(
                TowerSheetTest.build("A", TowerBench.BARE, TowerBench.BARE, 10, 4),
                TowerSheetTest.build("A", "full:0", TowerBench.FULL, 30, 8, "a"),
                TowerSheetTest.build("B", TowerBench.BARE, TowerBench.BARE, 20, 6),
                TowerSheetTest.build("B", "full:0", TowerBench.FULL, 50, 12, "b")));

        WavePowerReport.Yardstick yardstick = WavePowerReport.Yardstick.of(sheet);

        assertThat(yardstick.types()).isEqualTo(2);
        assertThat(yardstick.steps()).containsExactly(new WavePowerReport.Yardstick.Step(15, 5),
                new WavePowerReport.Yardstick.Step(40, 10));
    }

    @Test
    void theCheapestDefenceBuildsBareTowersBeforeItUpgradesOne() {
        WavePowerReport.Ground ground = ground(new double[] {1}, new double[] {1});

        double cost = ground.cost(List.of(new WavePowerReport.LanePower("", 8, 0)));

        assertThat(cost).isEqualTo(20, EXACT);
    }

    @Test
    void aTowerBoughtOnceEveryKindStandsPaysForBeingACopy() {
        WavePowerReport.Ground ground = ground(new double[] {1}, new double[] {1}, new double[] {1});

        double cost = ground.cost(List.of(new WavePowerReport.LanePower("", 12, 0)));

        assertThat(cost).isEqualTo(10 + 10 + 10 * GROWTH, EXACT);
    }

    @Test
    void aTowerThatReachesTwiceTheLaneHoldsTwiceAsMuch() {
        WavePowerReport.Ground ground = ground(new double[] {2}, new double[] {1});

        double cost = ground.cost(List.of(new WavePowerReport.LanePower("", 8, 0)));

        assertThat(cost).isEqualTo(10, EXACT);
    }

    @Test
    void aBoardWithNoRoomLeftPricesTheRestAsItsLastTower() {
        WavePowerReport.Ground ground = ground(new double[] {1});

        double cost = ground.cost(List.of(new WavePowerReport.LanePower("", 16, 0)));

        assertThat(cost).isEqualTo(40 + 6 * (30.0 / 6), EXACT);
    }

    @Test
    void aTowerServesOneLaneOnly() {
        WavePowerReport.Ground ground = ground(new double[] {1, 1}, new double[] {1, 1});
        WavePowerReport.LanePower lane = new WavePowerReport.LanePower("", 4, 0);

        assertThat(ground.cost(List.of(lane, lane))).isEqualTo(20, EXACT);
    }

    @Test
    void theTrendFollowsRoomsThatHalveEveryRound() {
        List<WavePowerReport.RoundPower> rounds = List.of(round(1, 100), round(2, 200), round(3, 400));

        WavePowerReport.Trend trend = WavePowerReport.Trend.through(rounds, new double[] {100, 100, 100});

        assertThat(trend.at(1)).isEqualTo(1.0, Offset.offset(1e-6));
        assertThat(trend.at(3)).isEqualTo(0.25, Offset.offset(1e-6));
    }

    @Test
    void aRoundWellUnderItsLevelsLineIsAGateAndOneFarOffItIsFlagged() {
        assertThat(WavePowerReport.verdict(1.0)).isEqualTo(TowerReport.OK);
        assertThat(WavePowerReport.verdict(0.85)).isEqualTo("gate");
        assertThat(WavePowerReport.verdict(0.5)).startsWith("WARN");
        assertThat(WavePowerReport.verdict(1.5)).startsWith("WARN");
    }

    private static WavePowerReport.Ground ground(double[]... reach) {
        List<Point> cells = IntStream.range(0, reach.length).mapToObj(x -> new Point(x, 0)).toList();
        return new WavePowerReport.Ground(TWO_STEPS, cells, List.of(reach));
    }

    private static WavePowerReport.RoundPower round(int round, double need) {
        return new WavePowerReport.RoundPower(round, List.of(), 0, Map.of(), 0, 0, need);
    }
}
