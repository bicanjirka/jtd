package td.save;

import td.damage.DamageMix;
import td.economy.EconomyState;

import java.util.List;

/**
 * A game between rounds, all of it (ADR 0021): the level, the round coming, the economy, every tower in the order it
 * was built with what it bought, paid and earned, the Ley Lines the player pinned, the rounds played so far, the
 * damage mix enemies adapt to, and the seed the waves draw their luck from. A tower's fight state ends with each wave,
 * so loading a save and playing on gives the same play, tick for tick, as playing on from where it was saved.
 *
 * @param nextWave the round that starts next, 1-based
 * @param seed     the game's seed; each wave's luck is drawn from it and the wave's number
 */
public record GameSave(String level, int nextWave, EconomyState economy, List<TowerSave> towers, long seed,
                       RoundHistory history, DamageMix damage, List<LeyLinePin> pins) {

    public GameSave {
        towers = List.copyOf(towers);
        pins = List.copyOf(pins);
    }

    /** A game with nothing played yet beyond its economy and towers: seed 0, no history, no damage, no pins. */
    public static GameSave of(String level, int nextWave, EconomyState economy, List<TowerSave> towers) {
        return new GameSave(level, nextWave, economy, towers, 0L, RoundHistory.none(), DamageMix.none(), List.of());
    }

    public GameSave withEconomy(EconomyState next) {
        return new GameSave(this.level, this.nextWave, next, this.towers, this.seed, this.history, this.damage,
                this.pins);
    }

    public GameSave withSeed(long next) {
        return new GameSave(this.level, this.nextWave, this.economy, this.towers, next, this.history, this.damage,
                this.pins);
    }

    public GameSave withHistory(RoundHistory next) {
        return new GameSave(this.level, this.nextWave, this.economy, this.towers, this.seed, next, this.damage,
                this.pins);
    }

    public GameSave withDamage(DamageMix next) {
        return new GameSave(this.level, this.nextWave, this.economy, this.towers, this.seed, this.history, next,
                this.pins);
    }

    public GameSave withPins(List<LeyLinePin> next) {
        return new GameSave(this.level, this.nextWave, this.economy, this.towers, this.seed, this.history, this.damage,
                next);
    }
}
