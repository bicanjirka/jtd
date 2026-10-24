package td.enemy;

import td.stat.EnemyStat;

import java.util.List;

/**
 * Everything a trace needs to check the numbers that follow it: health, every stat, traits and active effects.
 *
 * @param health  in units
 * @param traits  each trait's title
 * @param effects in kind order
 */
public record MobSnapshot(int health, List<StatReading> stats, List<String> traits, List<EffectState> effects) {

    public MobSnapshot {
        stats = List.copyOf(stats);
        traits = List.copyOf(traits);
        effects = List.copyOf(effects);
    }

    /** One stat: what the enemy is born with and what it resolves to now. */
    public record StatReading(EnemyStat stat, float base, float value) {
    }
}
