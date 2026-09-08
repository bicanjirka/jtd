package td;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellNormal;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerOne;
import td.util.GameWorld;
import td.util.RecordingGameHost;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises TowerPlacement directly rather than through GameEngine, so it can assert on the
 * shared click-highlight state (cleared by cancel()/unSelectTower()) that GameEngineTest has
 * no way to observe from the outside.
 */
class TowerPlacementTest {

    private static final int SCALE = 32;

    private static Cell[][] grid(int width, int height) {
        Cell[][] grid = new Cell[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                grid[i][j] = new CellNormal(i * SCALE, j * SCALE);
            }
        }
        return grid;
    }

    private static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }

    private static TowerPlacement newPlacement(Cell[][] grid, int credits) {
        GameWorld context = new GameWorld(new RecordingGameHost());
        context.setBoard(BoardGeometry.of(SCALE, grid.length, grid[0].length));
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
        Cell[][] grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.highlightCell(cellCenter(1), cellCenter(1));

        placement.cancel();

        assertThat(placement.isPlacing()).isFalse();
        assertThat(grid[1][1].getHighlight()).isEqualTo(Cell.highlightType.none);
    }

    @Test
    void highlightingACellMovesTheHighlightRatherThanStackingIt() {
        Cell[][] grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);

        placement.highlightCell(cellCenter(0), cellCenter(0));
        placement.highlightCell(cellCenter(1), cellCenter(1));

        assertThat(grid[0][0].getHighlight()).isEqualTo(Cell.highlightType.none);
        assertThat(grid[1][1].getHighlight()).isEqualTo(Cell.highlightType.place);
    }

    @Test
    void clickingAnOccupiedCellSelectsItsTowerAndHighlightsTheCell() {
        Cell[][] grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.mouseClicked(cellCenter(0), cellCenter(0));

        Tower selected = placement.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isNotNull();
        assertThat(grid[0][0].getHighlight()).isEqualTo(Cell.highlightType.select);
    }

    @Test
    void selectingATowerThenUnselectingClearsTheHighlight() {
        Cell[][] grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, 100);
        placement.start(TowerFactory.type.first, TowerOne.range);
        placement.mouseClicked(cellCenter(0), cellCenter(0));
        placement.mouseClicked(cellCenter(0), cellCenter(0));

        placement.unSelectTower();

        assertThat(grid[0][0].getHighlight()).isEqualTo(Cell.highlightType.none);
    }

    @Test
    void placingWithoutEnoughCreditsLeavesTheCellEmptyAndExitsPlacingMode() {
        Cell[][] grid = grid(3, 3);
        TowerPlacement placement = newPlacement(grid, TowerOne.price - 1);
        placement.start(TowerFactory.type.first, TowerOne.range);

        Tower selected = placement.mouseClicked(cellCenter(0), cellCenter(0));

        assertThat(selected).isNull();
        assertThat(grid[0][0].hasTower()).isFalse();
        assertThat(placement.isPlacing()).isFalse();
    }
}
