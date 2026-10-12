package td.tower.pulse;

import td.util.TickRate;

/**
 * How Toll builds on an enemy in the field: how high it stacks, how fast, and how long it lingers
 * once the enemy leaves.
 *
 * @param cap             the most stacks; {@code 0} for no Toll
 * @param stacksPerSecond how many stacks an enemy earns a second while inside
 * @param fadeTicks       how long, after the last tick inside, the stacks last
 */
public record TollSpec(int cap, float stacksPerSecond, int fadeTicks) {

    /** No Toll: the Pulse is not attuned. */
    public static TollSpec none() {
        return new TollSpec(0, 0f, 0);
    }

    public boolean isActive() {
        return this.cap > 0;
    }

    /** Ticks inside for one stack. */
    public int ticksPerStack() {
        return Math.max(1, Math.round(TickRate.TICKS_PER_SECOND / this.stacksPerSecond));
    }
}
