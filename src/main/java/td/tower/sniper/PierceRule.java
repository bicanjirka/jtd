package td.tower.sniper;

/**
 * How a piercing shot runs through the line past its target.
 *
 * @param extraReachCells how far past its range, in cells, the line runs
 * @param falloff         what each enemy after the first keeps of the one before's damage
 * @param halfWidthCells  how far from the line, in cells, an enemy is still on it
 */
public record PierceRule(float extraReachCells, float falloff, float halfWidthCells) {

    /** No piercing: the shot hits its target alone. */
    public static PierceRule none() {
        return new PierceRule(0f, 0f, 0f);
    }

    public boolean isActive() {
        return this.halfWidthCells > 0f;
    }
}
