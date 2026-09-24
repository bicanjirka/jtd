package td.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Credits, score and lives as one {@link EconomyState}, written from the EDT (buying, selling) and
 * the game-loop thread (kills, leaks). Each mutation fires exactly one notification, outside the
 * lock, since listeners re-enter.
 */
public class EconomyLedger {

    private static final Logger LOG = LoggerFactory.getLogger(EconomyLedger.class);

    private final List<EconomyListener> listeners = new CopyOnWriteArrayList<>();

    private volatile EconomyState economy = EconomyState.startingWith(0, 5);

    /**
     * Seeds the level's starting credits and lives, notifying like any change. Assigns inside the
     * lock so a concurrent kill's read-modify-write is not lost.
     */
    public void startEconomy(int startingCredits, int startingLives) {
        EconomyState seeded;
        synchronized (this) {
            seeded = EconomyState.startingWith(startingCredits, startingLives);
            this.economy = seeded;
        }
        LOG.debug("Economy seeded: {}", seeded);
        this.fireEconomyChangedEvent(seeded);
    }

    /** Applies one event atomically, firing one notification. */
    public void apply(EconomyDelta delta) {
        EconomyState updated;
        synchronized (this) {
            updated = this.economy.after(delta);
            this.economy = updated;
        }
        LOG.debug("Economy {} -> {}", delta, updated);
        this.fireEconomyChangedEvent(updated);
    }

    /**
     * Credits, score and lives from one snapshot. <strong>Use this when you need more than one of
     * them</strong>: separate reads can straddle a change and report a state that never existed.
     */
    public EconomyState state() {
        return this.economy;
    }

    public int getScore() {
        return this.economy.score();
    }

    public int getCredits() {
        return this.economy.credits();
    }

    public int getLives() {
        return this.economy.lives();
    }

    public boolean canPay(int amount) {
        return this.economy.canAfford(amount);
    }

    public boolean doPay(int amount) {
        EconomyState updated;
        synchronized (this) {
            if (!this.economy.canAfford(amount)) {
                return false;
            }
            updated = this.economy.after(EconomyDelta.credits(-amount));
            this.economy = updated;
        }
        LOG.debug("Credits -{} -> {}", amount, updated.credits());
        this.fireEconomyChangedEvent(updated);
        return true;
    }

    public void addEconomyListener(EconomyListener l) {
        this.listeners.add(l);
    }

    public void removeEconomyListener(EconomyListener l) {
        this.listeners.remove(l);
    }

    private void fireEconomyChangedEvent(EconomyState state) {
        for (EconomyListener l : this.listeners) {
            l.economyChanged(state);
        }
    }
}
