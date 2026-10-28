package td.enemy;

/** How the speed of a swing is spread over its length: maps progress through the swing to progress through its angle. */
public enum SwingCurve {
    /** Constant speed. */
    LINEAR,
    /** Slow at both ends, fastest midway: it stalls as it crosses the middle. */
    SMOOTH,
    /** Starts slow and speeds up. */
    ACCELERATING,
    /** Starts fast and slows down. */
    DECELERATING;

    /** @param progress how far through the swing, from 0 to 1 */
    public float angleFraction(float progress) {
        return switch (this) {
            case LINEAR -> progress;
            case SMOOTH -> progress * progress * (3f - 2f * progress);
            case ACCELERATING -> progress * progress;
            case DECELERATING -> progress * (2f - progress);
        };
    }
}
