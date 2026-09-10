package td.economy;

/**
 * Notified once per economy change, with the resulting state.
 * <p>
 * Fired from {@link EconomyLedger} on either the EDT (buying/selling) or the
 * {@code game-loop} thread (a kill or a leak), and always outside the ledger's lock. An
 * implementor that touches Swing must route through {@code SwingUtilities.invokeLater}.
 */
public interface EconomyListener {
    void economyChanged(EconomyState state);
}
