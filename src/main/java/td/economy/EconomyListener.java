package td.economy;

/**
 * Notified once per economy change, and once per payment refused, from the EDT or the game-loop thread and always
 * outside the ledger's lock. Swing work must go through {@code invokeLater}.
 */
public interface EconomyListener {
    void economyChanged(EconomyState state);

    /** A payment {@link EconomyLedger#doPay} refused for want of credits: something was not bought. */
    default void paymentRefused() {
    }
}
