package td.util;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.wave.PathNormal;
import td.wave.Vec2;
import td.wave.Wave;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoadedLevel is the single value the five correlated parts of a level cross threads in.
 * These cover the properties that makes it safe to publish: it is immutable, it models "no
 * level" as a value, and it cannot be changed through the list a caller handed it.
 */
class LoadedLevelTest {

    @Test
    void theNoLevelValueIsLoadableCheckedRatherThanNull() {
        LoadedLevel none = LoadedLevel.none();

        assertThat(none.isLoaded()).isFalse();
        assertThat(none.cells()).isSameAs(CellGrid.empty());
        assertThat(none.waveCount()).isZero();
        assertThat(none.path().points()).isEmpty();
    }

    @Test
    void theWaveListCannotBeChangedThroughTheListTheCallerPassedIn() {
        List<Wave> mutable = new ArrayList<>();
        LoadedLevel level = new LoadedLevel(CellGrid.of(3, 3, 32), BoardGeometry.of(32, 3, 3),
                new PathNormal(List.of(new Vec2(0, 0))), null, mutable);

        mutable.add(null);

        assertThat(level.waveCount()).isZero();
        assertThatThrownBy(() -> level.waves().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void replacingOnePartLeavesEveryOtherPartAlone() {
        LoadedLevel level = new LoadedLevel(CellGrid.of(4, 4, 32), BoardGeometry.of(32, 4, 4),
                new PathNormal(List.of(new Vec2(0, 0))), null, List.of());

        LoadedLevel moved = level.withPath(new PathNormal(List.of(new Vec2(9, 9))));

        assertThat(moved.cells()).isSameAs(level.cells());
        assertThat(moved.board()).isSameAs(level.board());
        assertThat(moved.path().points()).containsExactly(new Vec2(9, 9));
        assertThat(level.path().points()).containsExactly(new Vec2(0, 0));
    }
}
