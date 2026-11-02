package td.enemy;

/**
 * Faces the way it walks and sways about that heading as it goes, as a beast on the move does: a swing whose
 * middle is the path's direction rather than a fixed one.
 *
 * @param sway how far and how fast it turns to each side of its heading
 */
public record StrideMovement(SwingMovement sway) implements MovementBehavior {

    /** An even sway, slowest at each side. */
    public StrideMovement(float swayRadians, int strideTicks) {
        this(new SwingMovement(swayRadians, strideTicks, SwingCurve.SMOOTH));
    }

    /** The angle off its heading {@code ticks} after it began to walk. */
    public float angleAt(int ticks) {
        return this.sway.angleAt(ticks);
    }
}
