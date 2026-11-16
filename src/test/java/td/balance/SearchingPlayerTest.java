package td.balance;

import org.junit.jupiter.api.Test;
import td.level.LevelDefinition;
import td.tower.TowerFactory;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;
import td.wave.smoothing.ArcCornerSmoothing;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SearchingPlayerTest {

    private static final PlayerAction ANY = new PlayerAction.Sell(new Point(0, 0));

    private static SearchingPlayer.Option costing(int price) {
        return SearchingPlayer.Option.of(ANY, price);
    }

    /** Rounds that lost {@code leaks} lives to enemies that got away whole. */
    private static SearchingPlayer.Outcome leaking(int leaks) {
        return new SearchingPlayer.Outcome(leaks, leaks, 100);
    }

    @Test
    void theCheapestPurchaseThatLeavesNoLeakIsMade() {
        int picked = SearchingPlayer.pick(leaking(2), List.of(costing(10), costing(40), costing(25)),
                List.of(leaking(1), leaking(0), leaking(0)), SearchingPlayer.Policy.BEST_RATE);

        assertThat(picked).isEqualTo(2);
    }

    @Test
    void withNoneThatHoldsTheBestRateTakesWhatGainsMostForACredit() {
        int picked = SearchingPlayer.pick(leaking(4), List.of(costing(10), costing(40)),
                List.of(leaking(3), leaking(1)), SearchingPlayer.Policy.BEST_RATE);

        assertThat(picked).isZero();
    }

    @Test
    void withNoneThatHoldsTheBiggestStepTakesWhatGainsMostWhateverItCosts() {
        int picked = SearchingPlayer.pick(leaking(4), List.of(costing(10), costing(40)),
                List.of(leaking(3), leaking(1)), SearchingPlayer.Policy.BIGGEST_STEP);

        assertThat(picked).isEqualTo(1);
    }

    @Test
    void aLeakThatGetsAwayWithLessIsAGainThoughItCostsTheSameLives() {
        SearchingPlayer.Outcome now = new SearchingPlayer.Outcome(1, 1.0, 100);

        int picked = SearchingPlayer.pick(now, List.of(costing(10)),
                List.of(new SearchingPlayer.Outcome(1, 0.4, 100)), SearchingPlayer.Policy.BEST_RATE);

        assertThat(picked).isZero();
    }

    @Test
    void aPurchaseThatChangesNothingIsNotMade() {
        int picked = SearchingPlayer.pick(leaking(4), List.of(costing(10)), List.of(leaking(4)),
                SearchingPlayer.Policy.BEST_RATE);

        assertThat(picked).isNegative();
    }

    @Test
    void itBuildsUntilTheRoundIsHeldAndNeverWhatItMayNot() {
        BoardSurvey survey = BoardSurvey.of(LevelDefinition.of("One Round", 16, 11, List.of(
                        PathDefinition.smoothed(List.of(new Point(-1, 5), new Point(16, 5)),
                                List.of(new WaveDefinition("walker")), new ArcCornerSmoothing(0.4, 10))))
                .withStartingCredits(200)
                .withStartingLives(3));
        SearchingPlayer.Limits limits = new SearchingPlayer.Limits(EnumSet.of(TowerFactory.Type.SNIPER), false, 0);

        PlayRecord play = LevelPlay.play(survey, SearchingPlayer.spec("searching", limits,
                SearchingPlayer.Policy.BEST_RATE), 3L, LevelPlay.Pacing.standard());

        assertThat(play.result().leaks()).isZero();
        assertThat(play.towers()).isNotEmpty().allMatch(tower -> tower.type() == TowerFactory.Type.SNIPER
                && tower.nodes().isEmpty());
    }
}
