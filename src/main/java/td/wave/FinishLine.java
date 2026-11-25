package td.wave;

/**
 * How far along its path an enemy has to walk to be about to leak: past it, an enemy is the one a tower that picks a
 * single target takes first.
 */
public final class FinishLine {

    /** The share of its own path walked, from {@code 0} at its start to {@code 1} at its exit. */
    public static final float SHARE = 0.9f;

    private FinishLine() {
    }

    /** Whether an enemy that has walked {@code pathShare} of its path is past the line. */
    public static boolean isPassedAt(float pathShare) {
        return pathShare >= SHARE;
    }
}
