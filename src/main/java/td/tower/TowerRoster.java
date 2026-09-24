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
 * Towers on the board and their buy/sell lifecycle. The board geometry is supplied, since a tower
 * can be sold after a new level loaded.
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

    /** A live, read-only view that sees later additions. */
    public List<Tower> all() {
        return Collections.unmodifiableList(this.towers);
    }

    public void add(Tower t) {
        this.towers.add(t);
        this.recalculateAllStats();
        this.fireAdded(t);
    }

    /**
     * Recomputes every tower's stats, since any arrival or departure can change what the others
     * receive. Quadratic in tower count, which is fine for a user action over tens of towers;
     * measure before adding an index.
     */
    private void recalculateAllStats() {
        for (Tower t : this.towers) {
            t.recalculateStats();
        }
    }

    public void sell(Tower t) {
        BoardGeometry geometry = this.board.get();
        int cellX = geometry.cellX(t.getX());
        int cellY = geometry.cellY(t.getY());
        this.host.clearCell(cellX, cellY);
        t.doCleanup();
        this.towers.remove(t);
        this.recalculateAllStats();
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
