package td;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.cell.CellHighlight;
import td.fixtures.BoardFixtures;
import td.fixtures.WorldFixtures;
import td.tower.SniperTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** Tests the click-highlight state directly, which engine-level tests cannot observe. */
class TowerPlacementTest {

    private static CellGrid grid(int width, int height) {
        return CellGrid.of(width, height, BoardFixtures.SCALE);
    }

    private static TowerPlacement newPlacement(CellGrid grid, int credits) {
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, grid.width(), grid.height());
        context.economy().startEconomy(credits, 5);
        return new TowerPlacement(context, () -> grid);
    }

    @Test
    void startingPlacementMakesIsPlacingTrue() {
        TowerPlacement placement = newPlacement(grid(3, 3), 100);

        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);

        assertThat(placement.isPlacing()).isTrue();
    }

    @Test
    void cancellingPlacementClearsItAndTheHighlightedCell() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        placement.highlightCell(BoardFixtures.cellCenter(1), BoardFixtures.cellCenter(1));

        placement.cancel();

        assertThat(placement.isPlacing()).isFalse();
        assertThat(placement.highlight()).isEqualTo(CellHighlight.none());
    }

    @Test
    void highlightingACellMovesTheHighlightRatherThanStackingIt() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);

        placement.highlightCell(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        placement.highlightCell(BoardFixtures.cellCenter(1), BoardFixtures.cellCenter(1));

        assertThat(placement.highlight().isOn(0, 0)).isFalse();
        assertThat(placement.highlight().isOn(1, 1)).isTrue();
        assertThat(placement.highlight().kind()).isEqualTo(CellHighlight.Kind.PLACE);
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTowerAndHighlightsTheCell() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        placement.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        Optional<Tower> selected = placement.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        assertThat(selected).isPresent();
        assertThat(placement.highlight()).isEqualTo(CellHighlight.select(0, 0));
    }

    @Test
    void selectingATowerThenUnselectingClearsTheHighlight() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        placement.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));
        placement.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        placement.unSelectTower();

        assertThat(placement.highlight()).isEqualTo(CellHighlight.none());
    }

    @Test
    void resetClearsPlacementModeAndTheHighlight() {
        CellGrid bigGrid = grid(3, 3);
        TowerPlacement placement = newPlacement(bigGrid, 100);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        placement.highlightCell(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(2));

        placement.reset();

        assertThat(placement.isPlacing()).isFalse();
        assertThat(placement.highlight()).isEqualTo(CellHighlight.none());
    }

    @Test
    void resetPreventsAStaleHighlightFromCrashingWhenTheGridLaterShrinks() {
        CellGrid bigGrid = grid(3, 3);
        CellGrid[] currentGrid = {bigGrid};
        GameWorld context = WorldFixtures.newWorldOnBoard(BoardFixtures.SCALE, bigGrid.width(), bigGrid.height());
        context.economy().startEconomy(100, 5);
        TowerPlacement placement = new TowerPlacement(context, () -> currentGrid[0]);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        placement.highlightCell(BoardFixtures.cellCenter(2), BoardFixtures.cellCenter(2));

        placement.reset();
        currentGrid[0] = grid(1, 1);
        context.setBoard(BoardGeometry.of(BoardFixtures.SCALE, 1, 1));

        assertThatCode(() -> placement.highlightCell(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0)))
                .doesNotThrowAnyException();
    }

    @Test
    void theHoverCarriesThePriceOfTheTowerItWouldBuild() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 0);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);

        placement.highlightCell(BoardFixtures.cellCenter(1), BoardFixtures.cellCenter(1));

        assertThat(placement.highlight().price()).isEqualTo(SniperTower.PRICE);
    }

    @Test
    void clearingTheHighlightedCellDropsTheHighlightButClearingAnotherKeepsIt() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        placement.mouseClicked(BoardFixtures.cellCenter(0), BoardFixtures.cellCenter(0));

        placement.cellCleared(1, 1);
        CellHighlight afterAnother = placement.highlight();
        placement.cellCleared(0, 0);

        assertThat(afterAnother).isEqualTo(CellHighlight.select(0, 0));
        assertThat(placement.highlight()).isEqualTo(CellHighlight.none());
    }
}
