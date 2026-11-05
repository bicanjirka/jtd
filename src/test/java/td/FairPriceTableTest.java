package td;

import org.junit.jupiter.api.Test;
import td.FairPriceTable.MeasuredCopy;
import td.FairPriceTable.MeasuredLine;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeTier;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FairPriceTableTest {

    private static final TowerFactory.Type TOWER = TowerFactory.Type.SNIPER;
    private static final TowerFactory.Type OTHER_TOWER = TowerFactory.Type.SPLASH;
    private static final double NEAR = 1e-9;

    /** {@code credits} as a percent of the list price of {@link #TOWER}. */
    private static double percent(double credits) {
        return credits / TOWER.priceFor(0) * 100;
    }

    @Test
    void aCreditOnAnotherTowerSavesWhatEveryCopySavedOverWhatEveryCopyCost() {
        List<MeasuredCopy> copies = List.of(new MeasuredCopy(10, 2), new MeasuredCopy(30, 6));

        assertThat(FairPriceTable.livesPerCredit(copies)).hasValue(0.2);
    }

    @Test
    void whenAnotherTowerSavesNothingThereIsNoPriceToCompareWith() {
        List<MeasuredCopy> copies = List.of(new MeasuredCopy(10, -1), new MeasuredCopy(30, 0));

        assertThat(FairPriceTable.livesPerCredit(copies)).isEmpty();
    }

    @Test
    void aLineIsWorthWhatBuysTheSameLivesOnAnotherTower() {
        FairPriceTable table = new FairPriceTable(0.5);

        assertThat(table.linePrice(10)).isEqualTo(20);
    }

    @Test
    void aNodeIsPricedOnWhatItAddsToTheLineBeforeIt() {
        FairPriceTable table = new FairPriceTable(1);
        MeasuredLine first = new MeasuredLine(TOWER, UpgradeTier.HEAD_1, Set.of("a"), 2);
        MeasuredLine second = new MeasuredLine(TOWER, UpgradeTier.HEAD_2, Set.of("a", "b"), 5);

        assertThat(table.tierPercents(List.of(first, second)).get(UpgradeTier.HEAD_2)).isCloseTo(percent(3), within(NEAR));
    }

    @Test
    void aTierIsTheMeanOfItsNodesEvenWhereNoiseMadeOneNegative() {
        FairPriceTable table = new FairPriceTable(1);
        MeasuredLine gain = new MeasuredLine(TOWER, UpgradeTier.HEAD_1, Set.of("a"), 4);
        MeasuredLine loss = new MeasuredLine(TOWER, UpgradeTier.HEAD_1, Set.of("b"), -2);

        assertThat(table.tierPercents(List.of(gain, loss)).get(UpgradeTier.HEAD_1)).isCloseTo(percent(1), within(NEAR));
    }

    @Test
    void aLineOfAnotherTowerIsNotPartOfThisOneEvenWithTheSameNodeIds() {
        FairPriceTable table = new FairPriceTable(1);
        MeasuredLine other = new MeasuredLine(OTHER_TOWER, UpgradeTier.ATTUNE, Set.of("base"), 8);
        MeasuredLine own = new MeasuredLine(TOWER, UpgradeTier.HEAD_1, Set.of("base", "head"), 5);

        assertThat(table.tierPercents(List.of(other, own)).get(UpgradeTier.HEAD_1)).isCloseTo(percent(5), within(NEAR));
    }
}
