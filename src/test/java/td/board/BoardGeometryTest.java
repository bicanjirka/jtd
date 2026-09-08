package td.board;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardGeometryTest {

    @Test
    void derivesPixelBoundsFromScaleAndCellDimensions() {
        BoardGeometry board = BoardGeometry.of(32, 10, 5);

        assertThat(board.pixelWidth()).isEqualTo(320);
        assertThat(board.pixelHeight()).isEqualTo(160);
        assertThat(board.maxX()).isEqualTo(319);
        assertThat(board.maxY()).isEqualTo(159);
    }

    @Test
    void convertsAPixelPositionToItsCellCoordinates() {
        BoardGeometry board = BoardGeometry.of(32, 10, 10);

        assertThat(board.cellX(65)).isEqualTo(2);
        assertThat(board.cellY(31)).isEqualTo(0);
    }

    @Test
    void containsPixelIsTrueOnlyWithinTheBoardsPixelBounds() {
        BoardGeometry board = BoardGeometry.of(32, 2, 2);

        assertThat(board.containsPixel(0, 0)).isTrue();
        assertThat(board.containsPixel(63, 63)).isTrue();
        assertThat(board.containsPixel(64, 0)).isFalse();
        assertThat(board.containsPixel(-1, 0)).isFalse();
    }

    @Test
    void emptyIsTheZeroSizedIdentityBeforeALevelLoads() {
        BoardGeometry empty = BoardGeometry.empty();

        assertThat(empty.widthCells()).isZero();
        assertThat(empty.heightCells()).isZero();
        assertThat(empty.containsPixel(0, 0)).isFalse();
    }
}
