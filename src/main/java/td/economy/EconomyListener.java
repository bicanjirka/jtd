package td.economy;

/**
 * Notified once per economy change, from the EDT or the game-loop thread and always outside the
 * ledger's lock. Swing work must go through {@code invokeLater}.
 */
public interface EconomyListener {
    void economyChanged(EconomyState state);
}
