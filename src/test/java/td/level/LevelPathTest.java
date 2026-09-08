package td.level;

import org.junit.jupiter.api.Test;
import td.wave.Point;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class LevelPathTest {

    @Test
    void throughCornersExpandsAStraightRunIntoOneEntryPerCell() {
        List<Point> path = LevelPath.throughCorners(new Point(0, 2), new Point(4, 2));

        assertThat(path).extracting(Point::x, Point::y)
                .containsExactly(tuple(0, 2), tuple(1, 2), tuple(2, 2), tuple(3, 2), tuple(4, 2));
    }

    @Test
    void throughCornersExpandsAMultiCornerPathIntoOneEntryPerCell() {
        List<Point> path = LevelPath.throughCorners(new Point(0, 0), new Point(0, 2), new Point(2, 2));

        assertThat(path).extracting(Point::x, Point::y)
                .containsExactly(tuple(0, 0), tuple(0, 1), tuple(0, 2), tuple(1, 2), tuple(2, 2));
    }

    @Test
    void throughCornersAcceptsNegativeAndOffGridCoordinates() {
        List<Point> path = LevelPath.throughCorners(new Point(-2, 5), new Point(0, 5));

        assertThat(path).extracting(Point::x, Point::y)
                .containsExactly(tuple(-2, 5), tuple(-1, 5), tuple(0, 5));
    }

    @Test
    void throughCornersRejectsADiagonalCornerPair() {
        assertThatThrownBy(() -> LevelPath.throughCorners(new Point(0, 0), new Point(2, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void classicLoopCornersExpandToTheOriginalSixtyFiveCellPath() {
        List<Point> path = LevelPath.throughCorners(
                new Point(-1, 11), new Point(5, 11), new Point(5, 12), new Point(7, 12),
                new Point(7, 6), new Point(4, 6), new Point(4, 5), new Point(3, 5),
                new Point(3, 2), new Point(6, 2), new Point(6, 3), new Point(11, 3),
                new Point(11, 5), new Point(14, 5), new Point(14, 3), new Point(17, 3),
                new Point(17, 6), new Point(15, 6), new Point(15, 9), new Point(12, 9),
                new Point(12, 12), new Point(20, 12));

        int[] expectedX = {-1, 0, 1, 2, 3, 4, 5, 5, 6, 7, 7, 7, 7, 7, 7, 7, 6, 5, 4, 4, 3, 3, 3, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 11, 11, 12, 13, 14, 14, 14, 15, 16, 17, 17, 17, 17, 16, 15, 15, 15, 15, 14, 13, 12, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 20};
        int[] expectedY = {11, 11, 11, 11, 11, 11, 11, 12, 12, 12, 11, 10, 9, 8, 7, 6, 6, 6, 6, 5, 5, 4, 3, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 5, 5, 5, 5, 4, 3, 3, 3, 3, 4, 5, 6, 6, 6, 7, 8, 9, 9, 9, 9, 10, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12};

        assertThat(path).hasSize(65);
        for (int i = 0; i < expectedX.length; i++) {
            assertThat(path.get(i)).isEqualTo(new Point(expectedX[i], expectedY[i]));
        }
    }
}
