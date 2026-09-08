package td.tower;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.economy.EconomyDelta;
import td.economy.EconomyLedger;
import td.util.GameHost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * The towers currently on the board and their buy/sell lifecycle. {@code board} is a
 * supplier rather than one fixed {@link BoardGeometry} because a tower can be sold after
 * the level (and its geometry) that placed it has already loaded - callers always want
 * whatever geometry is current, not whatever it was at construction time.
 */
public class TowerRoster {

    private static final Logger LOG = LoggerFactory.getLogger(TowerRoster.class);

    private final List<Tower> towers = new CopyOnWriteArrayList<>();
    private final List<TowerListener> listeners = new CopyOnWriteArrayList<>();
    private final GameHost host;
    private final EconomyLedger economy;
    private final Supplier<BoardGeometry> board;

    public TowerRoster(GameHost host, EconomyLedger economy, Supplier<BoardGeometry> board) {
        this.host = host;
        this.economy = economy;
        this.board = board;
    }

    /**
     * A live, read-only view - callers (BoardRenderer, TowerUpgrade's proximity scan) must
     * see towers added after this was called, not a snapshot.
     */
    public List<Tower> all() {
        return Collections.unmodifiableList(this.towers);
    }

    public void add(Tower t) {
        this.towers.add(t);
        this.fireAdded(t);
    }

    public void sell(Tower t) {
        BoardGeometry geometry = this.board.get();
        int cellX = geometry.cellX(t.getX());
        int cellY = geometry.cellY(t.getY());
        this.host.clearCell(cellX, cellY);
        t.doCleanup();
        this.towers.remove(t);
        this.economy.apply(EconomyDelta.credits(t.getSellPrice()));
        this.fireRemoved(t);
        LOG.info("Tower sold: {} at ({},{}), refund={}", t.getType(), cellX, cellY, t.getSellPrice());
    }

    public void clear() {
        for (Tower t : new ArrayList<>(this.towers)) {
            BoardGeometry geometry = this.board.get();
            int cellX = geometry.cellX(t.getX());
            int cellY = geometry.cellY(t.getY());
            this.host.clearCell(cellX, cellY);
            t.doCleanup();
            this.towers.remove(t);
            this.fireRemoved(t);
        }
    }

    public void addListener(TowerListener l) {
        this.listeners.add(l);
    }

    public void removeListener(TowerListener l) {
        this.listeners.remove(l);
    }

    private void fireAdded(Tower t) {
        for (TowerListener l : this.listeners) {
            l.towerBuild(t);
        }
    }

    private void fireRemoved(Tower t) {
        for (TowerListener l : this.listeners) {
            l.towerRemoved(t);
        }
    }
}
