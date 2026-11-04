package td.tower;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.damage.AttackOrigin;
import td.economy.EconomyDelta;
import td.economy.EconomyLedger;
import td.enemy.EnemyMob;
import td.enemy.HitResult;
import td.util.GameHost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * Towers on the board and their buy/sell lifecycle. The board geometry is supplied, since a tower
 * can be sold after a new level loaded.
 */
public class TowerRoster {

    private static final Logger LOG = LoggerFactory.getLogger(TowerRoster.class);

    private final List<Tower> towers = new CopyOnWriteArrayList<>();
    private final List<TowerHitListener> hitListeners = new CopyOnWriteArrayList<>();
    private final GameHost host;
    private final EconomyLedger economy;
    private final Supplier<BoardGeometry> board;

    public TowerRoster(GameHost host, EconomyLedger economy, Supplier<BoardGeometry> board) {
        this.host = host;
        this.economy = economy;
        this.board = board;
    }

    /** A snapshot; later additions and removals don't show in it. */
    public List<Tower> all() {
        return List.copyOf(this.towers);
    }

    /** The tower whose hits and effects come from {@code origin}, as its name and cell: {@code Sniper@7,4}. */
    public Optional<String> attackerName(AttackOrigin origin) {
        BoardGeometry geometry = this.board.get();
        return this.towers.stream().filter(t -> t.origin() == origin).findFirst()
                .map(t -> t.displayName() + "@" + geometry.cellX(t.getX()) + "," + geometry.cellY(t.getY()));
    }

    /** What the next tower of {@code type} costs, given the copies already on the board. */
    public int priceOf(TowerFactory.Type type) {
        int copies = 0;
        for (Tower t : this.towers) {
            if (t.getType() == type) {
                copies++;
            }
        }
        return type.priceFor(copies);
    }

    public void addHitListener(TowerHitListener listener) {
        this.hitListeners.add(listener);
    }

    /** Tells every listener what {@code tower}'s hit on {@code enemy} did. */
    void reportHit(Tower tower, EnemyMob enemy, HitResult result) {
        for (TowerHitListener listener : this.hitListeners) {
            listener.landed(tower, enemy, result);
        }
    }

    public void add(Tower t) {
        this.towers.add(t);
        this.recalculateAllStats();
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
        BoardGeometry geometry = this.takeOffBoard(t);
        this.recalculateAllStats();
        this.economy.apply(EconomyDelta.credits(t.getSellPrice()));
        LOG.info("Tower sold: {} at ({},{}), refund={}", t.getType(), geometry.cellX(t.getX()), geometry.cellY(t.getY()),
                t.getSellPrice());
    }

    public void clear() {
        for (Tower t : new ArrayList<>(this.towers)) {
            this.takeOffBoard(t);
        }
    }

    /** Frees {@code t}'s cell and drops it; returns the board it stood on. */
    private BoardGeometry takeOffBoard(Tower t) {
        BoardGeometry geometry = this.board.get();
        this.host.clearCell(geometry.cellX(t.getX()), geometry.cellY(t.getY()));
        t.doCleanup();
        this.towers.remove(t);
        return geometry;
    }
}
