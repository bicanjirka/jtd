package td.util;

import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.enemy.EnemyCatalog;
import td.wave.Path;
import td.wave.PathColor;
import td.wave.PathNormal;
import td.wave.Wave;

import java.util.List;

/**
 * Everything a level install replaces, as one immutable value published through
 * {@link GameWorld#level()}. The parts are correlated: the board describes the grid, and the paths
 * marked its unbuildable cells.
 * <p>
 * Every path has the same number of waves, so {@link #waveCount()} is one round count and
 * {@link #wavesAt(int)} is everything round N starts. {@link #none()} means no level is loaded.
 */
public record LoadedLevel(CellGrid cells, BoardGeometry board, List<PathRuntime> paths, EnemyCatalog catalog) {

    // One degenerate path rather than none, so pathAt(0) resolves before any level loads.
    private static final LoadedLevel NONE = new LoadedLevel(CellGrid.empty(), BoardGeometry.empty(),
            List.of(new PathRuntime(new PathNormal(List.of()), List.of(), PathColor.DEFAULT)), EnemyCatalog.builtIn());

    /** Copies the list, so the caller cannot change it after publication. */
    public LoadedLevel {
        paths = List.copyOf(paths);
    }

    public static LoadedLevel none() {
        return NONE;
    }

    public boolean isLoaded() {
        return this.cells.isLoaded();
    }

    public int pathCount() {
        return this.paths.size();
    }

    public Path pathAt(int pathIndex) {
        return this.paths.get(pathIndex).path();
    }

    /** Rounds in this level; {@code 0} for {@link #none()}. */
    public int waveCount() {
        return this.paths.isEmpty() ? 0 : this.paths.getFirst().waves().size();
    }

    /**
     * @throws IndexOutOfBoundsException for a missing path or round
     */
    public Wave waveAt(int pathIndex, int round) {
        return this.paths.get(pathIndex).waves().get(round);
    }

    /**
     * Every path's wave at {@code round}, in path order.
     *
     * @throws IndexOutOfBoundsException for a missing round
     */
    public List<Wave> wavesAt(int round) {
        return this.paths.stream().map(pathRuntime -> pathRuntime.waves().get(round)).toList();
    }

    /** This level with one wave-less path replacing all others, for previews and tests. */
    public LoadedLevel withSinglePath(Path path) {
        return new LoadedLevel(this.cells, this.board, List.of(new PathRuntime(path, List.of(), PathColor.DEFAULT)),
                this.catalog);
    }

    public LoadedLevel withBoard(BoardGeometry board) {
        return new LoadedLevel(this.cells, board, this.paths, this.catalog);
    }

    public LoadedLevel withCatalog(EnemyCatalog catalog) {
        return new LoadedLevel(this.cells, this.board, this.paths, catalog);
    }
}
