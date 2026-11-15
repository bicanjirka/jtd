package td.balance;

import org.junit.jupiter.api.Test;
import td.tower.TowerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static td.balance.TowerSheetTest.build;
import static td.balance.TowerSheetTest.sheet;
import static td.balance.TowerSheetTest.weighing;

/** How measured builds become node worths, prices and verdicts. */
class TowerReportTest {

    private static final int TOWER_PRICE = 20;
    private static final int TIER = 2;
    private static final int WORTH = 8;
    private static final int VERDICT = 14;
    private static final List<List<String>> NO_PAIRS = List.of(List.of("tower", "build", "with", "withBuild", "cost"));

    private static List<List<String>> nodes(List<List<String>> builds) {
        return new TowerReport(sheet(builds), Map.of(), NO_PAIRS).nodes().rows();
    }

    @Test
    void aNodeOfALaterTierIsSetAgainstADearerCopyOfItsTower() {
        double step = TowerReport.premium(TIER + 1) / TowerReport.premium(TIER);

        assertThat(step).isCloseTo(TowerFactory.Type.COPY_PRICE_GROWTH, within(1e-9));
        assertThat(TowerReport.premium(0)).isGreaterThan(1);
    }

    @Test
    void aLaterTierAsksMoreOfItsNodesAndTheFirstAsksLessThanTheTower() {
        TowerReport.Pace first = TowerReport.Pace.of(0, TOWER_PRICE);
        TowerReport.Pace second = TowerReport.Pace.of(1, TOWER_PRICE);
        TowerReport.Pace third = TowerReport.Pace.of(2, TOWER_PRICE);

        assertThat(first.high()).isLessThan(TOWER_PRICE);
        assertThat(second.low()).isLessThanOrEqualTo(TOWER_PRICE);
        assertThat(second.high()).isGreaterThanOrEqualTo(TOWER_PRICE);
        assertThat(third.low()).isGreaterThan(second.target());
    }

    @Test
    void aNodeWorthLessThanItsTierAsksIsPricedAtTheTiersFloorAndCalledWeak() {
        TowerReport.Pace pace = TowerReport.Pace.of(TIER, TOWER_PRICE);

        TowerReport.Price price = TowerReport.priced(TowerReport.Reading.MEASURED, 1, TIER, TOWER_PRICE, pace.low());

        assertThat(price.suggested()).isEqualTo(pace.low());
        assertThat(price.fair()).isLessThan(pace.low());
        assertThat(price.verdict()).startsWith("WARN weak");
    }

    @Test
    void aNodeWorthMoreThanItsTierAsksIsPricedAtTheTiersCeilingAndCalledStrong() {
        TowerReport.Pace pace = TowerReport.Pace.of(TIER, TOWER_PRICE);

        TowerReport.Price price = TowerReport.priced(TowerReport.Reading.MEASURED, pace.high() * 2.0, TIER, TOWER_PRICE,
                pace.high());

        assertThat(price.suggested()).isEqualTo(pace.high());
        assertThat(price.verdict()).startsWith("WARN strong");
    }

    @Test
    void aNodeInsideItsTiersPaceIsRightAtItsFairPriceAndWarnedFarFromIt() {
        TowerReport.Pace pace = TowerReport.Pace.of(TIER, TOWER_PRICE);
        double worth = pace.target() / TowerReport.premium(TIER);

        TowerReport.Price right = TowerReport.priced(TowerReport.Reading.MEASURED, worth, TIER, TOWER_PRICE, pace.target());
        TowerReport.Price off = TowerReport.priced(TowerReport.Reading.MEASURED, worth, TIER, TOWER_PRICE, pace.target() * 2);

        assertThat(right.fair()).isEqualTo(pace.target());
        assertThat(right.verdict()).isEqualTo(TowerReport.OK);
        assertThat(off.verdict()).startsWith("WARN price").endsWith("-> " + pace.target());
    }

    @Test
    void aNodeTheBenchDoesNotShowIsListedUnmeasuredAndOneThatLowersItsBuildsIsABreach() {
        TowerReport.Pace pace = TowerReport.Pace.of(TIER, TOWER_PRICE);

        TowerReport.Price quiet = TowerReport.priced(TowerReport.Reading.QUIET, 0, TIER, TOWER_PRICE, 1);
        TowerReport.Price lowers = TowerReport.priced(TowerReport.Reading.LOWERS, -5, TIER, TOWER_PRICE, 1);

        assertThat(quiet.verdict()).startsWith("UNMEASURED");
        assertThat(quiet.suggested()).isEqualTo(pace.low());
        assertThat(lowers.verdict()).startsWith("BREACH");
    }

    @Test
    void aCeilingIsBrokenOnlyByARivalClearlyAboveIt() {
        Map<String, Double> rivals = new LinkedHashMap<>();
        rivals.put("level", 10 * (1 + TowerReport.NOISE / 2));
        rivals.put("above", 15.0);

        String verdict = TowerReport.judged(TowerReport.Relation.CEILING, 10, rivals);

        assertThat(verdict).startsWith("BREACH").contains("above x1.50").doesNotContain("level");
        assertThat(TowerReport.judged(TowerReport.Relation.CEILING, 10, Map.of("level", 10.0))).isEqualTo(TowerReport.OK);
    }

    @Test
    void aFloorIsBrokenOnlyByARivalClearlyBelowIt() {
        String verdict = TowerReport.judged(TowerReport.Relation.FLOOR, 10, Map.of("below", 5.0));

        assertThat(verdict).startsWith("BREACH").contains("below x0.50");
        assertThat(TowerReport.judged(TowerReport.Relation.FLOOR, 10, Map.of("above", 30.0))).isEqualTo(TowerReport.OK);
    }

    @Test
    void twoTowersMeantToBeLevelBreakTheRuleFarApartEitherWay() {
        double near = 10 * (TowerReport.NEAR + 1) / 2;

        assertThat(TowerReport.judged(TowerReport.Relation.EQUAL, 10, Map.of("near", near))).isEqualTo(TowerReport.OK);
        assertThat(TowerReport.judged(TowerReport.Relation.EQUAL, 10, Map.of("weak", 10 * TowerReport.NEAR / 2))).startsWith("BREACH");
        assertThat(TowerReport.judged(TowerReport.Relation.EQUAL, 10, Map.of("strong", 20 / TowerReport.NEAR))).startsWith("BREACH");
    }

    @Test
    void aClusterMemberAheadOfTheOthersEverywhereIsDominantAndTheOthersAreWeakerPicks() {
        TowerReport.Pick strong = new TowerReport.Pick("strong", 10, Map.of("stream", 10.0, "group", 10.0));
        TowerReport.Pick weak = new TowerReport.Pick("weak", 5, Map.of("stream", 5.0, "group", 5.0));

        List<String> verdicts = TowerReport.pickVerdicts(List.of(strong, weak));

        assertThat(verdicts.get(0)).startsWith("DOMINANT");
        assertThat(verdicts.get(1)).startsWith("WARN weaker pick");
        assertThat(strong.aheadAt(List.of(strong, weak))).containsExactlyInAnyOrder("stream", "group");
    }

    @Test
    void clusterMembersEachAheadSomewhereAndNearInAllAreBalanced() {
        TowerReport.Pick one = new TowerReport.Pick("one", 10, Map.of("stream", 14.0, "group", 6.0));
        TowerReport.Pick other = new TowerReport.Pick("other", 10 * (TowerReport.PICK_BAND + 1) / 2, Map.of("stream", 5.0, "group", 13.0));

        List<String> verdicts = TowerReport.pickVerdicts(List.of(one, other));

        assertThat(verdicts).containsExactly(TowerReport.OK, TowerReport.OK);
        assertThat(one.aheadAt(List.of(one, other))).containsExactly("stream");
        assertThat(other.aheadAt(List.of(one, other))).containsExactly("group");
    }

    @Test
    void aMemberFarAheadInAllButBehindSomewhereIsNotDominant() {
        TowerReport.Pick strong = new TowerReport.Pick("strong", 10, Map.of("stream", 18.0, "group", 2.0));
        TowerReport.Pick weak = new TowerReport.Pick("weak", 5, Map.of("stream", 4.0, "group", 6.0));

        List<String> verdicts = TowerReport.pickVerdicts(List.of(strong, weak));

        assertThat(verdicts.get(0)).isEqualTo(TowerReport.OK);
    }

    @Test
    void theWorthsOfATiersNodesAddUpToWhatTheTierAddsThoughTheNodesMultiplyEachOther() {
        double bare = 2;
        double a = 2;
        double b = 1.5;
        List<List<String>> rows = nodes(List.of(
                build("T", TowerBench.BARE, TowerBench.BARE, 10, bare),
                weighing(build("T", "line:a", TowerBench.LINE, 15, bare * a, "a"), "a", TowerBench.BARE),
                weighing(build("T", "line:b", TowerBench.LINE, 15, bare * b, "b"), "b", TowerBench.BARE),
                build("T", "full:0", TowerBench.FULL, 20, bare * a * b, "a", "b"),
                weighing(build("T", "without:a", TowerBench.WITHOUT, 15, bare * b, "b"), "a", "full:0"),
                weighing(build("T", "without:b", TowerBench.WITHOUT, 15, bare * a, "a"), "b", "full:0")));

        double worthOfA = Double.parseDouble(rows.get(0).get(WORTH));
        double worthOfB = Double.parseDouble(rows.get(1).get(WORTH));

        double rate = bare / 10;
        double lostWithoutA = bare * a * b - bare * b;
        double lostWithoutB = bare * a * b - bare * a;
        assertThat(worthOfA + worthOfB).isCloseTo((bare * a * b - bare) / rate, within(0.02));
        assertThat(worthOfA / worthOfB).isCloseTo(lostWithoutA / lostWithoutB, within(0.02));
    }

    @Test
    void aNodeOthersNeedIsNotCreditedWithWhatTheyAdd() {
        double bare = 2;
        double behind = 2;
        List<List<String>> rows = nodes(List.of(
                build("T", TowerBench.BARE, TowerBench.BARE, 10, bare),
                weighing(build("T", "line:gate", TowerBench.LINE, 15, bare, "gate"), "gate", TowerBench.BARE),
                weighing(build("T", "before:behind", TowerBench.BEFORE, 15, bare, "gate"), "behind", ""),
                weighing(build("T", "line:behind", TowerBench.LINE, 20, bare * behind, "gate", "behind"), "behind", "before:behind"),
                build("T", "full:0", TowerBench.FULL, 20, bare * behind, "gate", "behind"),
                weighing(build("T", "without:gate", TowerBench.WITHOUT, 10, bare), "gate", "full:0"),
                weighing(build("T", "without:behind", TowerBench.WITHOUT, 15, bare, "gate"), "behind", "full:0")));

        List<String> gate = rows.get(0);
        List<String> needsIt = rows.get(1);

        assertThat(gate.get(VERDICT)).startsWith("UNMEASURED");
        assertThat(Double.parseDouble(needsIt.get(WORTH))).isCloseTo((bare * behind - bare) / (bare / 10), within(0.02));
    }

    @Test
    void aNodeEveryBuildHoldsLessWithIsABreach() {
        double bare = 4;
        List<List<String>> rows = nodes(List.of(
                build("T", TowerBench.BARE, TowerBench.BARE, 10, bare),
                weighing(build("T", "line:trap", TowerBench.LINE, 15, bare / 2, "trap"), "trap", TowerBench.BARE),
                build("T", "full:0", TowerBench.FULL, 15, bare / 2, "trap"),
                weighing(build("T", "without:trap", TowerBench.WITHOUT, 10, bare), "trap", "full:0")));

        assertThat(rows.getFirst().get(VERDICT)).startsWith("BREACH");
    }
}
