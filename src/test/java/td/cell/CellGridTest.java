package td.cell;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellGridTest {

    private static final int SCALE = 32;

    @Test
    void anEmptyGridIsNotLoadedAndHasNoCells() {
        CellGrid grid = CellGrid.empty();

        assertThat(grid.isLoaded()).isFalse();
        assertThat(grid.width()).isZero();
        assertThat(grid.height()).isZero();
    }

    @Test
    void aGridOfZeroOrNegativeSizeIsTheEmptyGridRatherThanAnEmptyBoard() {
        assertThat(CellGrid.of(0, 5, SCALE).isLoaded()).isFalse();
        assertThat(CellGrid.of(5, -1, SCALE).isLoaded()).isFalse();
    }

    @Test
    void eachCellIsPositionedAtItsOwnTopLeftPixelCorner() {
        CellGrid grid = CellGrid.of(3, 2, SCALE);

        assertThat(grid.at(0, 0).getX()).isZero();
        assertThat(grid.at(0, 0).getY()).isZero();
        assertThat(grid.at(2, 1).getX()).isEqualTo(2 * SCALE);
        assertThat(grid.at(2, 1).getY()).isEqualTo(SCALE);
    }

    @Test
    void askingForACellOffTheBoardFailsLoudlyRatherThanReturningNothing() {
        CellGrid grid = CellGrid.of(2, 2, SCALE);

        assertThatThrownBy(() -> grid.at(2, 0))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("2x2");
        assertThatThrownBy(() -> grid.at(-1, 0)).isInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void containsAnswersWhetherACellExistsWithoutThrowing() {
        CellGrid grid = CellGrid.of(2, 3, SCALE);

        assertThat(grid.contains(1, 2)).isTrue();
        assertThat(grid.contains(2, 0)).isFalse();
        assertThat(grid.contains(0, -1)).isFalse();
    }

    @Test
    void everyCellIsVisitedOnce() {
        CellGrid grid = CellGrid.of(3, 4, SCALE);
        List<Cell> visited = new ArrayList<>();

        grid.forEach(visited::add);

        assertThat(visited).hasSize(12).doesNotHaveDuplicates();
    }

    @Test
    void visitingAnEmptyGridDoesNothingRatherThanFailing() {
        List<Cell> visited = new ArrayList<>();

        CellGrid.empty().forEach(visited::add);

        assertThat(visited).isEmpty();
    }

    @Test
    void twoGridsDoNotShareCells() {
        CellGrid first = CellGrid.of(2, 2, SCALE);
        CellGrid second = CellGrid.of(2, 2, SCALE);

        first.at(0, 0).enable(false);

        assertThat(second.at(0, 0).buildable()).isTrue();
    }
}
