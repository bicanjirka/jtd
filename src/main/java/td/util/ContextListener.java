package td.util;

/**
 * Listener herniho kontextu<br>
 * Zachytava udalosti zmena penezniho konta
 * a zmena zivotu
 *
 * @author Juras
 *
 */
public interface ContextListener {
    /**
     * Zmena penez
     */
    void moneyChanged();

    /**
     * Zmena zivotu
     */
    void livesChanged();
}
