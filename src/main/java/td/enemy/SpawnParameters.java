package td.enemy;

/**
 * A mob's birth parameters as one value - its spawn delay (already converted to ticks), health,
 * bounty, the size/speed multipliers its {@code SpawnShape} applies, and its fixed lateral
 * offset from the path centre - so {@link AbstractEnemyMob}'s constructor takes one argument
 * instead of growing a new parameter for every spawn-shape mechanism. {@link #atSlot} is the
 * identity form: today's spawn behaviour, unmultiplied and on the path centre.
 */
public record SpawnParameters(int delayTicks, int health, int price, float sizeMultiplier, float speedMultiplier,
                               double lateralOffset) {

    /**
     * How many ticks of spawn delay one slot of wave ordering is worth, at a mob's own speed.
     * Lives here, not in {@link AbstractEnemyMob}, so the slot-position-to-ticks conversion
     * happens where a spawn shape's per-member delay spacing (a fractional slot position) is
     * computed, not inside the mob itself.
     */
    private static final float DELAY_TICKS_PER_SLOT = 22.4f;

    /**
     * Today's spawn behaviour: no shape multipliers, no offset, at the definition's own speed.
     */
    public static SpawnParameters atSlot(double slotPosition, float baseSpeed, int health, int price) {
        return of(slotPosition, baseSpeed, health, price, 1f, 1f, 0.0);
    }

    public static SpawnParameters of(double slotPosition, float baseSpeed, int health, int price,
                                      float sizeMultiplier, float speedMultiplier, double lateralOffset) {
        float speed = baseSpeed * speedMultiplier;
        // A slot at position 0, or a mob with no speed of its own (an ability-spawned egg), is
        // never delayed - guarded explicitly rather than dividing by a possibly-zero speed,
        // which would round to Integer.MAX_VALUE ticks instead of the intended zero.
        int delayTicks = slotPosition <= 0 || speed <= 0
                ? 0
                : (int) Math.round(DELAY_TICKS_PER_SLOT * slotPosition / speed);
        return new SpawnParameters(delayTicks, health, price, sizeMultiplier, speedMultiplier, lateralOffset);
    }
}
