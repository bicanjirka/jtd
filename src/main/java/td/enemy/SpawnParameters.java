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

    /** Spawn-delay ticks per slot of wave ordering, at the mob's own speed, unless a wave says otherwise. */
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
        return of(slotPosition, DEFAULT_DELAY_TICKS_PER_SLOT, baseSpeed, health, price, sizeMultiplier,
                speedMultiplier, localOffset, pathIndex);
    }

    /** With {@code delayTicksPerSlot} ticks between slots, where the others use the default. */
    public static SpawnParameters of(double slotPosition, float delayTicksPerSlot, float baseSpeed, int health,
                                      int price, float sizeMultiplier, float speedMultiplier, Vec2 localOffset,
                                      int pathIndex) {
        float speed = baseSpeed * speedMultiplier;
        // Guarded rather than dividing by a zero speed, which would round to Integer.MAX_VALUE.
        int delayTicks = slotPosition <= 0 || speed <= 0
                ? 0
                : (int) Math.round(delayTicksPerSlot * slotPosition / speed);
        return new SpawnParameters(delayTicks, health, price, sizeMultiplier, speedMultiplier, localOffset, pathIndex);
    }
}
