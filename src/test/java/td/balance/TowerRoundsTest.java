package td.balance;

import org.junit.jupiter.api.Test;
import td.tower.TowerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** How a round's bank and XP become the best defence a tower can stand. */
class TowerRoundsTest {

    private static final int PRICE = 20;
    private static final TowerRounds.Build BARE = new TowerRounds.Build("bare", -1, 0, 1.0);
    private static final TowerRounds.Build TIER_TWO = new TowerRounds.Build("full:2", 2, 40, 5.0);

    @Test
    void eachCopyCostsTheSurchargeMoreThanTheOneBefore() {
        int second = (int) Math.round(PRICE * TowerFactory.Type.COPY_PRICE_GROWTH);

        assertThat(TowerRounds.copiesCost(PRICE, 2)).isEqualTo(PRICE + second);
    }

    @Test
    void aTierOpensAtItsGateAndTierZeroIsAlwaysOpen() {
        List<Integer> gates = List.of(60, 120, 200);

        assertThat(TowerRounds.openTier(gates, 0)).isZero();
        assertThat(TowerRounds.openTier(gates, 119)).isEqualTo(1);
        assertThat(TowerRounds.openTier(gates, 200)).isEqualTo(3);
    }

    @Test
    void aBuildOfAClosedTierIsNeverStoodWhateverTheBank() {
        TowerRounds.Defence closed = TowerRounds.best(List.of(BARE, TIER_TWO), PRICE, 1000, 1);
        TowerRounds.Defence open = TowerRounds.best(List.of(BARE, TIER_TWO), PRICE, 1000, 2);

        assertThat(closed.build()).isEqualTo("bare");
        assertThat(open.build()).isEqualTo("full:2");
    }

    @Test
    void theBankCapsHowManyCopiesAreBought() {
        int bank = TowerRounds.copiesCost(PRICE, 3);

        TowerRounds.Defence defence = TowerRounds.best(List.of(BARE), PRICE, bank, 0);

        assertThat(defence.copies()).isEqualTo(3);
        assertThat(defence.cost()).isEqualTo(bank);
    }

    @Test
    void theRicherLaneIsTheOneWithTheMostXpAtTheEnd() {
        List<List<String>> sheet = List.of(List.of("level", "round", "bank", "xp"),
                List.of("Other", "1", "10", "0 | 0"),
                List.of("Here", "1", "50", "0 | 0"),
                List.of("Here", "2", "80", "9 | 15"));

        List<TowerRounds.Round> rounds = TowerRounds.roundsOf(sheet, "Here");

        assertThat(rounds).containsExactly(new TowerRounds.Round(1, 50, 0, 0), new TowerRounds.Round(2, 80, 15, 9));
    }
}
