package td.cell;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class CellTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void startsBuildableAndEmpty() {
        Cell cell = new Cell(0, 0);

        assertThat(cell.buildable()).isTrue();
        assertThat(cell.hasTower()).isFalse();
        assertThat(cell.getTower()).isNull();
    }

    @Test
    void setTowerOccupiesTheCellAndMakesItUnbuildable() {
        Cell cell = new Cell(0, 0);
        Tower tower = TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0);

        cell.setTower(tower);

        assertThat(cell.hasTower()).isTrue();
        assertThat(cell.getTower()).isSameAs(tower);
        assertThat(cell.buildable()).isFalse();
    }

    @Test
    void setTowerOnAnAlreadyOccupiedCellIsIgnored() {
        Cell cell = new Cell(0, 0);
        Tower first = TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0);
        Tower second = TowerFactory.createTower(TowerFactory.Type.SPLASH, context, 0, 0);

        cell.setTower(first);
        cell.setTower(second);

        assertThat(cell.getTower()).isSameAs(first);
    }

    @Test
    void unSetTowerClearsTheCellAndMakesItBuildableAgain() {
        Cell cell = new Cell(0, 0);
        cell.setTower(TowerFactory.createTower(TowerFactory.Type.SNIPER, context, 0, 0));

        cell.unSetTower();

        assertThat(cell.hasTower()).isFalse();
        assertThat(cell.getTower()).isNull();
        assertThat(cell.buildable()).isTrue();
    }

    @Test
    void enableTogglesBuildability() {
        Cell cell = new Cell(0, 0);

        cell.setBuildable(false);
        assertThat(cell.buildable()).isFalse();

        cell.setBuildable(true);
        assertThat(cell.buildable()).isTrue();
    }
}
