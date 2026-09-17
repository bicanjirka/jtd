package td.tower;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.economy.EconomyLedger;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TowerRosterTest {

    private final RecordingGameHost host = new RecordingGameHost();
    private final EconomyLedger economy = new EconomyLedger();
    private final TowerRoster roster = new TowerRoster(host, economy, () -> BoardGeometry.of(32, 10, 10));
    private final GameWorld context = new GameWorld(host);

    private Tower aTower() {
        return TowerFactory.createTower(TowerFactory.Type.first, context, 2, 3);
    }

    @Test
    void addingATowerNotifiesListenersAndAppearsInAll() {
        Tower tower = aTower();
        List<Tower> built = new ArrayList<>();
        roster.addListener(new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
            }

            @Override
            public void towerBuild(Tower t) {
                built.add(t);
            }
        });

        roster.add(tower);

        assertThat(built).containsExactly(tower);
        assertThat(roster.all()).containsExactly(tower);
    }

    @Test
    void allReturnsALiveViewThatSeesLaterAdditions() {
        List<Tower> view = roster.all();
        assertThat(view).isEmpty();

        roster.add(aTower());

        assertThat(view).hasSize(1);
    }

    @Test
    void sellingATowerRefundsCreditsClearsItsCellAndNotifiesListeners() {
        Tower tower = aTower();
        roster.add(tower);
        List<Tower> removed = new ArrayList<>();
        roster.addListener(new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
                removed.add(t);
            }

            @Override
            public void towerBuild(Tower t) {
            }
        });

        roster.sell(tower);

        assertThat(roster.all()).doesNotContain(tower);
        assertThat(removed).containsExactly(tower);
        assertThat(economy.getCredits()).isEqualTo(tower.getSellPrice());
        assertThat(host.lastClearedCell).containsExactly(2, 3);
    }

    @Test
    void clearRemovesEveryTowerAndClearsEachCell() {
        roster.add(aTower());
        roster.add(TowerFactory.createTower(TowerFactory.Type.first, context, 5, 6));

        roster.clear();

        assertThat(roster.all()).isEmpty();
    }

    @Test
    void removedListenerStopsReceivingEvents() {
        List<Tower> built = new ArrayList<>();
        TowerListener listener = new TowerListener() {
            @Override
            public void towerRemoved(Tower t) {
            }

            @Override
            public void towerBuild(Tower t) {
                built.add(t);
            }
        };
        roster.addListener(listener);
        roster.removeListener(listener);

        roster.add(aTower());

        assertThat(built).isEmpty();
    }
}
