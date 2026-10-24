package td.enemy;

import td.damage.Damage;
import td.util.ThreadConfined;

/**
 * One mob's health in damage units: what it has left, its maximum, everything that has landed on
 * it, and the regeneration too small to land yet.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
final class MobHealth {

    private final int max;

    private int current;
    private long damageTaken;
    // The part of a heal too small to land as a whole unit this tick; paid out once it adds up.
    private float regenCarry;

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

    /**
     * Adds a tick of regeneration, landing only whole units, never above the maximum.
     *
     * @return whether health changed
     */
    boolean regenerate(float perTick) {
        this.regenCarry += perTick;
        int whole = (int) this.regenCarry;
        this.regenCarry -= whole;
        int healed = Math.min(this.max, this.current + whole);
        boolean changed = healed != this.current;
        this.current = healed;
        return changed;
    }

    void restore() {
        this.current = this.max;
    }
}
