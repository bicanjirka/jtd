package td.tower.aura;

/**
 * Rally: an enemy stepping on the aura's path spot speeds up every tower it buffs.
 *
 * @param spotCells     how near the spot, in cells, an enemy must be to step on it
 * @param attackSpeed   the attack speed each buffed tower gains
 * @param ticks         how long the gain lasts
 * @param cooldownTicks how long the spot rests after it rallies
 */
public record RallyRule(float spotCells, float attackSpeed, int ticks, int cooldownTicks) {

    /** No Rally. */
    public static RallyRule none() {
        return new RallyRule(0f, 0f, 0, 0);
    }

    public boolean isActive() {
        return this.ticks > 0;
    }
}
