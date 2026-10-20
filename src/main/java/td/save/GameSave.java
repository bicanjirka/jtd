package td.save;

import td.economy.EconomyState;

import java.util.List;

/**
 * A game between rounds, enough to play on from there: the level, the round coming, the economy and every tower in
 * the order it was built, with what it bought and earned. What a tower holds only while it fights (a charge, a
 * stock, an aim) is not kept, nor is the randomness, so a loaded game plays as the saved one could have, not tick
 * for tick the same.
 *
 * @param nextWave the round that starts next, 1-based
 */
public record GameSave(String level, int nextWave, EconomyState economy, List<TowerSave> towers) {

    public GameSave {
        towers = List.copyOf(towers);
    }
}
