package td.util;

import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.enemy.EnemyCatalog;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Wave;

import java.util.List;

/**
 * Everything installing a level replaces, as one immutable value.
 * <p>
 * These five are correlated, not independent: the wave index counts into {@link #waves()},
 * {@link #board()}'s dimensions describe the same board {@link #cells()} lays out, and
 * {@link #path()} is what marked those cells unbuildable. They were once five separate
 * {@code volatile} fields across {@code GameEngine} and {@code GameWorld}, written one after
 * another by {@code loadLevel} - which let the game-loop thread read a board from the incoming
 * level and a cell grid from the outgoing one. CLAUDE.md 3 rule 1 is the rule that applies:
 * correlated fields cross as one immutable snapshot behind a single {@code volatile}, which is
 * {@link GameWorld#level()}.
 * <p>
 * {@link #none()} is the "no level loaded" value, so nothing models that state as {@code null}.
 */
public record LoadedLevel(CellGrid cells, BoardGeometry board, Path path,
                          EnemyCatalog catalog, List<Wave> waves) {

    private static final LoadedLevel NONE = new LoadedLevel(
            CellGrid.empty(), BoardGeometry.empty(), new PathNormal(List.of()),
            EnemyCatalog.builtIn(), List.of());

    /** Defensive copy, so the list a caller built cannot be changed after publication. */
    public LoadedLevel {
        waves = List.copyOf(waves);
    }

    /** The state before any level is loaded, and after one is torn down. */
    public static LoadedLevel none() {
        return NONE;
    }

    /** Whether a level's board is actually installed, as opposed to {@link #none()}. */
    public boolean isLoaded() {
        return this.cells.isLoaded();
    }

    /** How many waves this level runs. */
    public int waveCount() {
        return this.waves.size();
    }

    /**
     * The wave at {@code index}.
     *
     * @throws IndexOutOfBoundsException if there is no such wave - callers that cannot rule
     *                                   that out first should check {@link #waveCount()}
     */
    public Wave waveAt(int index) {
        return this.waves.get(index);
    }

    /** This level with a different board, for a display-only world - see {@link GameWorld}. */
    public LoadedLevel withBoard(BoardGeometry board) {
        return new LoadedLevel(this.cells, board, this.path, this.catalog, this.waves);
    }

    /** This level with a different path, for a display-only world - see {@link GameWorld}. */
    public LoadedLevel withPath(Path path) {
        return new LoadedLevel(this.cells, this.board, path, this.catalog, this.waves);
    }

    /** This level with a different enemy catalog - see {@link GameWorld}. */
    public LoadedLevel withCatalog(EnemyCatalog catalog) {
        return new LoadedLevel(this.cells, this.board, this.path, catalog, this.waves);
    }
}
