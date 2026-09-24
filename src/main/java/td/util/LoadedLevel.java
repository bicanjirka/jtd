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
 * Everything installing a level replaces, as one immutable value.
 * <p>
 * These are correlated, not independent: {@link #board()}'s dimensions describe the same board
 * {@link #cells()} lays out, and every path in {@link #paths()} is what marked those cells
 * unbuildable. They were once five separate {@code volatile} fields across {@code GameEngine}
 * and {@code GameWorld}, written one after another by {@code loadLevel} - which let the
 * game-loop thread read a board from the incoming level and a cell grid from the outgoing one.
 * CLAUDE.md 3 rule 1 is the rule that applies: correlated fields cross as one immutable snapshot
 * behind a single {@code volatile}, which is {@link GameWorld#level()}.
 * <p>
 * A level's waves run as synchronized rounds across every path (see {@code td/wave/CLAUDE.md}):
 * every {@link PathRuntime} in {@link #paths()} holds the same number of waves - enforced at
 * authoring time by {@code td.level.LevelDefinition}'s own compact constructor - so
 * {@link #waveCount()} is one shared round count, and {@link #wavesAt(int)} is the one accessor
 * for "everything that starts when round N starts."
 * <p>
 * {@link #none()} is the "no level loaded" value, so nothing models that state as {@code null}.
 */
public record LoadedLevel(CellGrid cells, BoardGeometry board, List<PathRuntime> paths, EnemyCatalog catalog) {

    // One degenerate path rather than none, so pathAt(0) resolves before any level loads.
    private static final LoadedLevel NONE = new LoadedLevel(CellGrid.empty(), BoardGeometry.empty(),
            List.of(new PathRuntime(new PathNormal(List.of()), List.of(), PathColor.DEFAULT)), EnemyCatalog.builtIn());

    /**
     * Defensive copy, so the list a caller built cannot be changed after publication.
     */
    public LoadedLevel {
        paths = List.copyOf(paths);
    }

    /**
     * The state before any level is loaded, and after one is torn down.
     */
    public static LoadedLevel none() {
        return NONE;
    }

    /**
     * Whether a level's board is actually installed, as opposed to {@link #none()}.
     */
    public boolean isLoaded() {
        return this.cells.isLoaded();
    }

    /**
     * How many paths this level has.
     */
    public int pathCount() {
        return this.paths.size();
    }

    /**
     * The geometry of the path at {@code pathIndex}.
     */
    public Path pathAt(int pathIndex) {
        return this.paths.get(pathIndex).path();
    }

    /**
     * How many rounds this level runs - the one wave count every path agrees on. {@code 0} for
     * {@link #none()}, whose one degenerate path has no waves.
     */
    public int waveCount() {
        return this.paths.isEmpty() ? 0 : this.paths.getFirst().waves().size();
    }

    /**
     * One path's wave at {@code round}.
     *
     * @throws IndexOutOfBoundsException if there is no such path or round - callers that cannot
     *                                   rule that out first should check {@link #pathCount()}/
     *                                   {@link #waveCount()}
     */
    public Wave waveAt(int pathIndex, int round) {
        return this.paths.get(pathIndex).waves().get(round);
    }

    /**
     * Every path's wave at {@code round}, in path order - what starting round {@code round}
     * spawns, together.
     *
     * @throws IndexOutOfBoundsException if there is no such round - callers that cannot rule
     *                                   that out first should check {@link #waveCount()}
     */
    public List<Wave> wavesAt(int round) {
        return this.paths.stream().map(pathRuntime -> pathRuntime.waves().get(round)).toList();
    }

    /**
     * This level with a single path replacing every other one, at path index {@code 0}, with no
     * waves and the default color - the single-lane convenience {@link GameWorld#setPath} needs
     * for a display-only world (a wave-preview panel's off-board enemies) or a test that only
     * cares about geometry.
     */
    public LoadedLevel withSinglePath(Path path) {
        return new LoadedLevel(this.cells, this.board, List.of(new PathRuntime(path, List.of(), PathColor.DEFAULT)),
                this.catalog);
    }

    /**
     * This level with a different board, for a display-only world - see {@link GameWorld}.
     */
    public LoadedLevel withBoard(BoardGeometry board) {
        return new LoadedLevel(this.cells, board, this.paths, this.catalog);
    }

    /**
     * This level with a different enemy catalog - see {@link GameWorld}.
     */
    public LoadedLevel withCatalog(EnemyCatalog catalog) {
        return new LoadedLevel(this.cells, this.board, this.paths, catalog);
    }
}
