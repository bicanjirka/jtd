package td.effect;

import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.stat.StatModifiers;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a tower has made of the burn or poison pool it feeds, carried by the pool: how much it holds,
 * how fast it marks the enemy, what freezing it does and what it does to the enemy's stats while it
 * burns. A pool two towers feed holds the stronger of each.
 *
 * @param capFactor        the most a pool holds, as a multiple of its strongest application
 * @param stackRate        how many times as fast it earns the marks a pool earns
 * @param freezeBurstShare the share of what is left of it that lands at once when a freeze puts it out
 * @param carried          what the enemy's stats suffer while it burns
 */
public record PoolTuning(float capFactor, int stackRate, float freezeBurstShare, StatModifiers<EnemyStat> carried) {

    private static final PoolTuning STANDARD = new PoolTuning(2f, 1, 0.5f, StatModifiers.none());

    /** What every pool is, unless a tower tunes it. */
    public static PoolTuning standard() {
        return STANDARD;
    }

    public PoolTuning withCapFactor(float capFactor) {
        return new PoolTuning(capFactor, this.stackRate, this.freezeBurstShare, this.carried);
    }

    public PoolTuning withStackRate(int stackRate) {
        return new PoolTuning(this.capFactor, stackRate, this.freezeBurstShare, this.carried);
    }

    public PoolTuning withFreezeBurstShare(float freezeBurstShare) {
        return new PoolTuning(this.capFactor, this.stackRate, freezeBurstShare, this.carried);
    }

    /** This tuning, also carrying {@code more}: they add up. */
    public PoolTuning withCarried(StatModifiers<EnemyStat> more) {
        return new PoolTuning(this.capFactor, this.stackRate, this.freezeBurstShare, this.carried.plus(more));
    }

    /** The stronger of each setting of this and {@code other}. */
    PoolTuning strongerWith(PoolTuning other) {
        return new PoolTuning(Math.max(this.capFactor, other.capFactor), Math.max(this.stackRate, other.stackRate),
                Math.max(this.freezeBurstShare, other.freezeBurstShare), strongerCarried(this.carried, other.carried));
    }

    /** Per stat, the modifier whose flat amount is the larger either way. */
    private static StatModifiers<EnemyStat> strongerCarried(StatModifiers<EnemyStat> a, StatModifiers<EnemyStat> b) {
        if (a.equals(b) || b.isNone()) {
            return a;
        }
        if (a.isNone()) {
            return b;
        }
        Set<EnemyStat> stats = EnumSet.noneOf(EnemyStat.class);
        stats.addAll(a.stats());
        stats.addAll(b.stats());
        StatModifiers<EnemyStat> stronger = StatModifiers.none();
        for (EnemyStat stat : stats) {
            StatModifier mine = a.get(stat);
            StatModifier theirs = b.get(stat);
            stronger = stronger.and(stat, Math.abs(mine.flat()) >= Math.abs(theirs.flat()) ? mine : theirs);
        }
        return stronger;
    }
}
