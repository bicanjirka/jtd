package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.economy.EconomyLedger;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerRosterTest {

    private final RecordingGameHost host = new RecordingGameHost();
    private final EconomyLedger economy = new EconomyLedger();
    private final TowerRoster roster = new TowerRoster(host, economy, () -> BoardGeometry.of(BoardFixtures.SCALE, 10, 10));
    private final GameWorld context = WorldFixtures.newWorld(host);

    private Tower aTower() {
        return TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 2, 3);
    }

    @Test
    void addingATowerAppearsInAll() {
        Tower tower = aTower();

        roster.add(tower);

        assertThat(roster.all()).containsExactly(tower);
    }

    @Test
    void allIsASnapshotThatDoesNotSeeLaterAdditions() {
        List<Tower> snapshot = roster.all();

        roster.add(aTower());

        assertThat(snapshot).isEmpty();
    }

    @Test
    void sellingATowerRefundsCreditsAndClearsItsCell() {
        Tower tower = aTower();
        roster.add(tower);

        roster.sell(tower);

        assertThat(roster.all()).doesNotContain(tower);
        assertThat(economy.getCredits()).isEqualTo(tower.getSellPrice());
        assertThat(host.lastClearedCell).containsExactly(2, 3);
    }

    @Test
    void clearRemovesEveryTowerAndClearsEachCell() {
        roster.add(aTower());
        roster.add(TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 5, 6));

        roster.clear();

        assertThat(roster.all()).isEmpty();
    }
}
