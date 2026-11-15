package td.balance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** How a suggested price is written into a tower's source. */
class TowerPriceWriterTest {

    private static final String SOURCE = """
            public static final int PRICE = 20;
            private static final UpgradeNode FIRST = UpgradeNode.of("t.first", "First", 5)
                    .inTier(1);
            private static final UpgradeNode SECOND = UpgradeNode.of(Nodes.SHARED_ID, "First II", 9);
            private static final UpgradeNode QUOTED = UpgradeNode.of("t.quoted", "Dragon's (Hot) Breath", 7);
            """;

    @Test
    void aNodesPriceIsReplacedAndEveryOtherNumberIsLeft() {
        String written = TowerPriceWriter.nodePriced(SOURCE, "First", 12);

        assertThat(written).contains("UpgradeNode.of(\"t.first\", \"First\", 12)")
                .contains("\"First II\", 9)").contains("PRICE = 20;").contains(".inTier(1)");
    }

    @Test
    void aNodeNamedThroughAConstantOrWithSignsInItsNameIsFound() {
        String shared = TowerPriceWriter.nodePriced(SOURCE, "First II", 30);
        String quoted = TowerPriceWriter.nodePriced(SOURCE, "Dragon's (Hot) Breath", 40);

        assertThat(shared).contains("UpgradeNode.of(Nodes.SHARED_ID, \"First II\", 30)");
        assertThat(quoted).contains("\"Dragon's (Hot) Breath\", 40)");
    }

    @Test
    void aSourceWithoutTheNodeComesBackUnchanged() {
        String written = TowerPriceWriter.nodePriced(SOURCE, "Missing", 12);

        assertThat(written).isEqualTo(SOURCE);
    }

    @Test
    void aTowersListPriceIsReplacedAndItsNodesAreLeft() {
        String written = TowerPriceWriter.towerPriced(SOURCE, 15);

        assertThat(written).contains("PRICE = 15;").contains("\"First\", 5)");
    }
}
