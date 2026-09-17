package td;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.cell.CellNormal;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerOne;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exercises TowerPlacement directly rather than through GameEngine, so it can assert on the
 * shared click-highlight state (cleared by cancel()/unSelectTower()) that GameEngineTest has
 * no way to observe from the outside.
 */
class TowerPlacementTest {

    private static final int SCALE = 32;

    private static CellGrid grid(int width, int height) {
        return CellGrid.of(width, height, SCALE);
    }

    private static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }

    private static TowerPlacement newPlacement(CellGrid grid, int credits) {
        GameWorld context = new GameWorld(new RecordingGameHost());
        context.setBoard(BoardGeometry.of(SCALE, grid.width(), grid.height()));
        context.startEconomy(credits, 5);
        return new TowerPlacement(context, () -> grid);
    }

    @Test
    void startingPlacementMakesIsPlacingTrue() {
        TowerPlacement placement = newPlacement(grid(3, 3), 100);

        placement.start(TowerFactory.type.first, TowerOne.range);

        assertThat(placement.isPlacing()).isTrue();
    }

    @Test
    void cancellingPlacementClearsItAndTheHighlightedCell() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.highlightCell(cellCenter(1), cellCenter(1));

        placement.cancel();

        assertThat(placement.isPlacing()).isFalse();
        assertThat(grid.at(1, 1).getHighlight()).isEqualTo(Cell.highlightType.none);
    }

    @Test
    void highlightingACellMovesTheHighlightRatherThanStackingIt() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);

        placement.highlightCell(cellCenter(0), cellCenter(0));
        placement.highlightCell(cellCenter(1), cellCenter(1));

        assertThat(grid.at(0, 0).getHighlight()).isEqualTo(Cell.highlightType.none);
        assertThat(grid.at(1, 1).getHighlight()).isEqualTo(Cell.highlightType.place);
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTowerAndHighlightsTheCell() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.mouseClicked(cellCenter(0), cellCenter(0));

        Optional<Tower> selected = placement.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isPresent();
        assertThat(grid.at(0, 0).getHighlight()).isEqualTo(Cell.highlightType.select);
    }

    @Test
    void selectingATowerThenUnselectingClearsTheHighlight() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.mouseClicked(cellCenter(0), cellCenter(0));
        placement.mouseClicked(cellCenter(0), cellCenter(0));

        placement.unSelectTower();

        assertThat(grid.at(0, 0).getHighlight()).isEqualTo(Cell.highlightType.none);
    }

    @Test
    void resetClearsPlacementModeAndTheStaleHighlightWithoutTouchingTheGrid() {
        CellGrid bigGrid = grid(3, 3);
        TowerPlacement placement = newPlacement(bigGrid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.highlightCell(cellCenter(2), cellCenter(2));

        placement.reset();

        assertThat(placement.isPlacing()).isFalse();
        // the highlighted cell belonged to the grid being discarded - reset() must not
        // dereference it, since a real reload can replace it with a smaller one
        assertThat(bigGrid.at(2, 2).getHighlight()).isEqualTo(Cell.highlightType.place);
    }

    @Test
    void resetPreventsAStaleHighlightFromCrashingWhenTheGridLaterShrinks() {
        // mirrors how GameEngine really wires this: one TowerPlacement, a Supplier whose
        // answer changes when a new (possibly smaller) level replaces the grid
        CellGrid bigGrid = grid(3, 3);
        CellGrid[] currentGrid = {bigGrid};
        GameWorld context = new GameWorld(new RecordingGameHost());
        context.setBoard(BoardGeometry.of(SCALE, bigGrid.width(), bigGrid.height()));
        context.startEconomy(100, 5);
        TowerPlacement placement = new TowerPlacement(context, () -> currentGrid[0]);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.highlightCell(cellCenter(2), cellCenter(2));

        placement.reset();
        currentGrid[0] = grid(1, 1);
        context.setBoard(BoardGeometry.of(SCALE, 1, 1));

        assertThatCode(() -> placement.highlightCell(cellCenter(0), cellCenter(0)))
                .doesNotThrowAnyException();
    }

    @Test
    void placingWithoutEnoughCreditsLeavesTheCellEmptyAndExitsPlacingMode() {
        CellGrid grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, TowerOne.price - 1);
        placement.start(TowerFactory.type.first, TowerOne.range);

        Optional<Tower> selected = placement.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isEmpty();
        assertThat(grid.at(0, 0).hasTower()).isFalse();
        assertThat(placement.isPlacing()).isFalse();
    }
}
