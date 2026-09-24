package td.enemy;

import td.wave.Vec2;

/**
 * A mob's birth parameters as one value - its spawn delay (already converted to ticks), health,
 * bounty, the size/speed multipliers its {@code SpawnShape} applies, its formation offset, and
 * which of the level's paths it walks - so {@link AbstractEnemyMob}'s constructor takes one
 * argument instead of growing a new parameter for every spawn-shape mechanism.
 * {@link #atSlot(double, float, int, int)} is the identity form: today's spawn behaviour,
 * unmultiplied, on the path centre, on path 0.
 * <p>
 * {@code localOffset} is relative to the mob's own spawn-facing direction, not world space:
 * {@code x} is forward (along the path's direction at the spawn point), {@code y} is lateral
 * (perpendicular to it). {@link AbstractEnemyMob} rotates it into a fixed world-space vector
 * once, at construction - see its own doc comment for why that has to happen only once.
 */
public record SpawnParameters(int delayTicks, int health, int price, float sizeMultiplier, float speedMultiplier,
                               Vec2 localOffset, int pathIndex) {

    /**
     * How many ticks of spawn delay one slot of wave ordering is worth, at a mob's own speed.
     * Lives here, not in {@link AbstractEnemyMob}, so the slot-position-to-ticks conversion
     * happens where a spawn shape's per-member delay spacing (a fractional slot position) is
     * computed, not inside the mob itself.
     */
    private static final float DELAY_TICKS_PER_SLOT = 22.4f;

    /**
     * Today's spawn behaviour: no shape multipliers, no offset, at the definition's own speed,
     * on path 0.
     */
    public static SpawnParameters atSlot(double slotPosition, float baseSpeed, int health, int price) {
        return atSlot(slotPosition, baseSpeed, health, price, 0);
    }

    /**
     * Today's spawn behaviour, on a specific path - what an ability-driven spawn (the Warden's
     * egg) uses to inherit its parent's own path instead of defaulting to path 0.
     */
    public static SpawnParameters atSlot(double slotPosition, float baseSpeed, int health, int price, int pathIndex) {
        return of(slotPosition, baseSpeed, health, price, 1f, 1f, new Vec2(0, 0), pathIndex);
    }

    /**
     * On path 0 - the only lane every pre-existing caller (every wave, before this feature,
     * spawned on) needs to name.
     */
    public static SpawnParameters of(double slotPosition, float baseSpeed, int health, int price,
                                      float sizeMultiplier, float speedMultiplier, Vec2 localOffset) {
        return of(slotPosition, baseSpeed, health, price, sizeMultiplier, speedMultiplier, localOffset, 0);
    }

    public static SpawnParameters of(double slotPosition, float baseSpeed, int health, int price,
                                      float sizeMultiplier, float speedMultiplier, Vec2 localOffset, int pathIndex) {
        float speed = baseSpeed * speedMultiplier;
        // Guarded rather than dividing by a zero speed, which would round to Integer.MAX_VALUE.
        int delayTicks = slotPosition <= 0 || speed <= 0
                ? 0
                : (int) Math.round(DELAY_TICKS_PER_SLOT * slotPosition / speed);
        return new SpawnParameters(delayTicks, health, price, sizeMultiplier, speedMultiplier, localOffset, pathIndex);
    }
}
