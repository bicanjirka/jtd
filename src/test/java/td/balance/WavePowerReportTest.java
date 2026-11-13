package td.balance;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import td.wave.Point;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WavePowerReportTest {

    private static final Offset<Double> EXACT = Offset.offset(1e-9);
    private static final PaperYardstick TWO_STEPS = new PaperYardstick(3,
            List.of(new PaperYardstick.Step(10, 4), new PaperYardstick.Step(40, 10)));

    @Test
    void aTowerThatReachesHalfALaneLandsHalfOfWhatWalksPastAtOnce() {
        WavePowerReport.LanePower lane = new WavePowerReport.LanePower("", 1, new PowerBench.Passage(200, 0));

        assertThat(lane.share(0.5)).isEqualTo(0.5, EXACT);
    }

    @Test
    void aTowerFiresForAsLongAsTheStreamKeepsComing() {
        WavePowerReport.LanePower lane = new WavePowerReport.LanePower("", 1, new PowerBench.Passage(100, 300));

        assertThat(lane.share(0.2)).isEqualTo((0.2 * 100 + 300) / 400, EXACT);
    }

    @Test
    void theCheapestDefenceBuildsBareTowersBeforeItUpgradesOne() {
        WavePowerReport.Ground ground = new WavePowerReport.Ground(TWO_STEPS,
                List.of(new Point(0, 0), new Point(1, 0)), List.of(new double[] {1}, new double[] {1}));

        double cost = ground.cost(List.of(new WavePowerReport.LanePower("", 8, new PowerBench.Passage(100, 0))));

        assertThat(cost).isEqualTo(20, EXACT);
    }

    @Test
    void aBoardWithNoRoomLeftPricesTheRestAsItsLastTower() {
        WavePowerReport.Ground ground = new WavePowerReport.Ground(TWO_STEPS,
                List.of(new Point(0, 0)), List.of(new double[] {1}));

        double cost = ground.cost(List.of(new WavePowerReport.LanePower("", 16, new PowerBench.Passage(100, 0))));

        assertThat(cost).isEqualTo(40 + 6 * (30.0 / 6), EXACT);
    }

    @Test
    void aTowerServesOneLaneOnly() {
        WavePowerReport.Ground ground = new WavePowerReport.Ground(TWO_STEPS,
                List.of(new Point(0, 0), new Point(1, 0)), List.of(new double[] {1, 1}, new double[] {1, 1}));
        WavePowerReport.LanePower lane = new WavePowerReport.LanePower("", 4, new PowerBench.Passage(100, 0));

        assertThat(ground.cost(List.of(lane, lane))).isEqualTo(20, EXACT);
    }

    @Test
    void theTrendFollowsRoomsThatHalveEveryRound() {
        List<WavePowerReport.RoundPower> rounds = List.of(round(1, 100), round(2, 200), round(3, 400));

        WavePowerReport.Trend trend = WavePowerReport.Trend.through(rounds, new double[] {100, 100, 100});

        assertThat(trend.at(1)).isEqualTo(1.0, Offset.offset(1e-6));
        assertThat(trend.at(3)).isEqualTo(0.25, Offset.offset(1e-6));
    }

    private static WavePowerReport.RoundPower round(int round, double need) {
        return new WavePowerReport.RoundPower(round, List.of(), 0, Map.of(), 0, 0, need);
    }
}
