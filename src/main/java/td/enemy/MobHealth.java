package td.enemy;

import td.damage.Damage;
import td.util.ThreadConfined;

/**
 * One mob's health in damage units: what it has left, its maximum, everything that has landed on
 * it, and the regeneration too small to land yet.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class MobHealth {

    /** How often health is written down for a rewind to look back on. */
    static final int SAMPLE_TICKS = 5;

    private final int max;
    // A ring of past health, one entry every SAMPLE_TICKS, reaching as far back as the longest rewind.
    private final int[] history = new int[RewindAction.LONGEST_TICKS / SAMPLE_TICKS + 1];

    private int current;
    private long damageTaken;
    // The part of a heal too small to land as a whole unit this tick; paid out once it adds up.
    private float regenCarry;
    private int samples;

    MobHealth(int units) {
        this.max = units;
        this.current = units;
    }

    int current() {
        return this.current;
    }

    int max() {
        return this.max;
    }

    float fraction() {
        return (float) this.current / this.max;
    }

    long damageTaken() {
        return this.damageTaken;
    }

    boolean depleted() {
        return this.current <= 0;
    }

    /** Takes {@code hit} off health, capped at what is left so overkill is not counted as taken. */
    Damage lose(Damage hit) {
        Damage landed = hit.cappedAt(this.current);
        this.current -= landed.amount();
        this.damageTaken += landed.amount();
        return landed;
    }

    /** The whole units a tick of {@code perTick} regeneration brings, keeping the part of a unit that is not yet whole. */
    int regenerationDue(float perTick) {
        this.regenCarry += perTick;
        int whole = (int) this.regenCarry;
        this.regenCarry -= whole;
        return whole;
    }

    /** Adds {@code units} to health, never above the maximum; what does not fit is lost. */
    void heal(int units) {
        this.current = Math.min(this.max, this.current + units);
    }

    /** Writes down health as it is now; called every {@link #SAMPLE_TICKS} live ticks. */
    void record() {
        this.history[this.samples % this.history.length] = this.current;
        this.samples++;
    }

    /** What health was about {@code ticks} ago, or the oldest written down if that is further back; now if nothing is. */
    int ticksAgo(int ticks) {
        if (this.samples == 0) {
            return this.current;
        }
        int oldest = Math.max(0, this.samples - this.history.length);
        int index = Math.max(oldest, this.samples - 1 - Math.round((float) ticks / SAMPLE_TICKS));
        return this.history[index % this.history.length];
    }

    /** Takes {@code units} off health with no hit behind it, never below nothing. */
    void decay(int units) {
        this.current = Math.max(0, this.current - units);
    }

    void restore() {
        this.current = this.max;
    }
}
