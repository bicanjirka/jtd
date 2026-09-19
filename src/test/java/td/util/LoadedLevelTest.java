package td.util;

import org.junit.jupiter.api.Test;
import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.enemy.EnemyCatalog;
import td.wave.PathColor;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LoadedLevel is the single value the correlated parts of a level cross threads in. These cover
 * the properties that makes it safe to publish: it is immutable, it models "no level" as a
 * value, and it cannot be changed through the list a caller handed it.
 */
class LoadedLevelTest {

    @Test
    void theNoLevelValueIsLoadableCheckedRatherThanNull() {
        LoadedLevel none = LoadedLevel.none();

        assertThat(none.isLoaded()).isFalse();
        assertThat(none.cells()).isSameAs(CellGrid.empty());
        assertThat(none.waveCount()).isZero();
        // One degenerate path, not zero - see LoadedLevel.NONE's own doc comment: a mob built
        // before any level loads still resolves pathAt(0) rather than throwing.
        assertThat(none.pathCount()).isEqualTo(1);
        assertThat(none.pathAt(0).points()).isEmpty();
    }

    @Test
    void thePathListCannotBeChangedThroughTheListTheCallerPassedIn() {
        List<PathRuntime> mutable = new ArrayList<>();
        LoadedLevel level = new LoadedLevel(CellGrid.of(3, 3, 32), BoardGeometry.of(32, 3, 3),
                mutable, EnemyCatalog.builtIn());

        mutable.add(new PathRuntime(new PathNormal(List.of(new Vec2(0, 0))), List.of(), PathColor.DEFAULT));

        assertThat(level.pathCount()).isZero();
        assertThatThrownBy(() -> level.paths().add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void replacingOnePartLeavesEveryOtherPartAlone() {
        List<PathRuntime> paths = List.of(
                new PathRuntime(new PathNormal(List.of(new Vec2(0, 0))), List.of(), PathColor.DEFAULT));
        LoadedLevel level = new LoadedLevel(CellGrid.of(4, 4, 32), BoardGeometry.of(32, 4, 4),
                paths, EnemyCatalog.builtIn());

        LoadedLevel moved = level.withSinglePath(new PathNormal(List.of(new Vec2(9, 9))));

        assertThat(moved.cells()).isSameAs(level.cells());
        assertThat(moved.board()).isSameAs(level.board());
        assertThat(moved.pathAt(0).points()).containsExactly(new Vec2(9, 9));
        assertThat(level.pathAt(0).points()).containsExactly(new Vec2(0, 0));
    }
}
