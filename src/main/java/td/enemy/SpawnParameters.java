package td.enemy;

import td.wave.Vec2;

/**
 * A mob's birth parameters: spawn delay in ticks, health, bounty, size and speed multipliers,
 * formation offset and path index. {@link #atSlot(double, float, int, int)} is the plain form.
 * <p>
 * {@code localOffset} is relative to the spawn-point facing: {@code x} forward, {@code y} lateral.
 */
public record SpawnParameters(int delayTicks, int health, int price, float sizeMultiplier, float speedMultiplier,
                               Vec2 localOffset, int pathIndex) {

    /**
     * Ticks one slot of spacing is worth to an enemy of speed 1, unless a wave says otherwise: a slot is a distance,
     * so a slower enemy takes longer to walk it.
     */
    public static final float DEFAULT_DELAY_TICKS_PER_SLOT = 22.4f;

    /** No multipliers or offset, on path 0. */
    public static SpawnParameters atSlot(double slotPosition, float baseSpeed, int health, int price) {
        return atSlot(slotPosition, baseSpeed, health, price, 0);
    }

    /** No multipliers or offset, on the given path. */
    public static SpawnParameters atSlot(double slotPosition, float baseSpeed, int health, int price, int pathIndex) {
        return of(slotPosition, baseSpeed, health, price, 1f, 1f, new Vec2(0, 0), pathIndex);
    }

    /** On path 0. */
    public static SpawnParameters of(double slotPosition, float baseSpeed, int health, int price,
                                      float sizeMultiplier, float speedMultiplier, Vec2 localOffset) {
        return of(slotPosition, baseSpeed, health, price, sizeMultiplier, speedMultiplier, localOffset, 0);
    }

    public static SpawnParameters of(double slotPosition, float baseSpeed, int health, int price,
                                      float sizeMultiplier, float speedMultiplier, Vec2 localOffset, int pathIndex) {
        float speed = baseSpeed * speedMultiplier;
        // Guarded rather than dividing by a zero speed, which would round to Integer.MAX_VALUE.
        double delayTicks = speed <= 0 ? 0 : DEFAULT_DELAY_TICKS_PER_SLOT * slotPosition / speed;
        return afterTicks(delayTicks, health, price, sizeMultiplier, speedMultiplier, localOffset, pathIndex);
    }

    /**
     * Spawning {@code delayTicks} ticks after its wave starts, rounded to whole ticks; none if that is not positive.
     */
    public static SpawnParameters afterTicks(double delayTicks, int health, int price, float sizeMultiplier,
                                             float speedMultiplier, Vec2 localOffset, int pathIndex) {
        int ticks = delayTicks <= 0 ? 0 : (int) Math.round(delayTicks);
        return new SpawnParameters(ticks, health, price, sizeMultiplier, speedMultiplier, localOffset, pathIndex);
    }
}
