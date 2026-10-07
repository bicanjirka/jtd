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

    private static final int ATTUNED_CAP = 5;
    private static final int ATTUNED_FADE_TICKS = Math.round(1f * TickRate.TICKS_PER_SECOND);

    /** No Toll: the Pulse is not attuned. */
    public static TollSpec none() {
        return new TollSpec(0, 0f, 0);
    }

    /** Attune's Toll: up to five stacks, one a second, fading a second after the enemy leaves. */
    public static TollSpec attuned() {
        return new TollSpec(ATTUNED_CAP, 1f, ATTUNED_FADE_TICKS);
    }

    public boolean isActive() {
        return this.cap > 0;
    }

    /** Ticks inside for one stack. */
    public int ticksPerStack() {
        return Math.max(1, Math.round(TickRate.TICKS_PER_SECOND / this.stacksPerSecond));
    }

    public TollSpec withCap(int cap) {
        return new TollSpec(cap, this.stacksPerSecond, this.fadeTicks);
    }

    /** Stacks building {@code factor} times as fast. */
    public TollSpec buildingFasterBy(float factor) {
        return new TollSpec(this.cap, this.stacksPerSecond * factor, this.fadeTicks);
    }

    /** Stacks lasting {@code ticks} longer after the enemy leaves. */
    public TollSpec lingeringLonger(int ticks) {
        return new TollSpec(this.cap, this.stacksPerSecond, this.fadeTicks + ticks);
    }
}
