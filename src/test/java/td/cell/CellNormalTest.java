package td.cell;

import org.junit.jupiter.api.Test;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.Context;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

class CellNormalTest {

    private final Context context = new Context(new RecordingGameHost());

    @Test
    void startsBuildableAndEmpty() {
        CellNormal cell = new CellNormal(0, 0, context);

        assertThat(cell.buildable()).isTrue();
        assertThat(cell.hasTower()).isFalse();
        assertThat(cell.getTower()).isNull();
    }

    @Test
    void setTowerOccupiesTheCellAndMakesItUnbuildable() {
        CellNormal cell = new CellNormal(0, 0, context);
        Tower tower = TowerFactory.createTower(TowerFactory.type.first, context, 0, 0);

        cell.setTower(tower);

        assertThat(cell.hasTower()).isTrue();
        assertThat(cell.getTower()).isSameAs(tower);
        assertThat(cell.buildable()).isFalse();
    }

    @Test
    void setTowerOnAnAlreadyOccupiedCellIsIgnored() {
        CellNormal cell = new CellNormal(0, 0, context);
        Tower first = TowerFactory.createTower(TowerFactory.type.first, context, 0, 0);
        Tower second = TowerFactory.createTower(TowerFactory.type.second, context, 0, 0);

        cell.setTower(first);
        cell.setTower(second);

        assertThat(cell.getTower()).isSameAs(first);
    }

    @Test
    void unSetTowerClearsTheCellAndMakesItBuildableAgain() {
        CellNormal cell = new CellNormal(0, 0, context);
        cell.setTower(TowerFactory.createTower(TowerFactory.type.first, context, 0, 0));

        cell.unSetTower();

        assertThat(cell.hasTower()).isFalse();
        assertThat(cell.getTower()).isNull();
        assertThat(cell.buildable()).isTrue();
    }

    @Test
    void enableTogglesBuildability() {
        CellNormal cell = new CellNormal(0, 0, context);

        cell.enable(false);
        assertThat(cell.buildable()).isFalse();

        cell.enable(true);
        assertThat(cell.buildable()).isTrue();
    }
}
