package td.util;

import td.economy.EconomyState;

public interface ContextListener {
    void economyChanged(EconomyState state);
}
