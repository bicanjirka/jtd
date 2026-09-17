package td.economy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Owns the player's credits/score/lives as one {@link EconomyState} and the
 * synchronized-compute/notify-outside-lock discipline required because it is written from
 * both the EDT (buying/selling a tower) and the {@code game-loop} thread (a kill or a leak) -
 * see CLAUDE.md §3 (Threading). Every mutation fires exactly one {@link EconomyListener}
 * notification, and never while holding the lock, since a listener re-enters and touches
 * Swing.
 */
public class EconomyLedger {

    private static final Logger LOG = LoggerFactory.getLogger(EconomyLedger.class);

    private final List<EconomyListener> listeners = new CopyOnWriteArrayList<>();

    private volatile EconomyState economy = EconomyState.startingWith(0, 5);

    /**
     * Seeds the economy at the start of a level - credits and lives are both the level's own,
     * not carried over from whatever ran before. Fires like any other economy change since
     * listeners are already registered by the time a level loads.
     * <p>
     * Assigns inside the lock for the same reason {@link #apply} and {@link #doPay} compute
     * inside it: a concurrent kill or leak on the {@code game-loop} thread is a
     * read-modify-write, and an unsynchronized assignment here would let one of the two be
     * silently lost.
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

    /**
     * Applies a game event's effect on credits/score/lives as one atomic move, firing exactly
     * one notification for it - replacing what used to be up to three separate mutations
     * (see EconomyDelta.kill/leak).
     */
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
     * Credits, score and lives as one consistent snapshot.
     * <p>
     * <strong>A caller that needs more than one of them must use this</strong>, not two of the
     * scalar accessors below. {@link EconomyState} exists precisely because those three are
     * correlated, and reading them one at a time is two or three separate reads of the volatile
     * - which can straddle a kill or a purchase and report a state that never existed. The
     * listener path was always given the whole value; this is the same guarantee for a caller
     * that polls.
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
