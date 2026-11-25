package td.ui;

/**
 * The board's easing curves. Every timed visual shapes its 0..1 progress through these, so a moment on one tower moves
 * like a moment on any other. Each clamps its input, so a progress a little past its end stays at the end.
 */
final class Easing {

    private Easing() {
    }

    static double clamp01(double t) {
        return Math.clamp(t, 0, 1);
    }

    /** Starts fast and settles: what is thrown or released. */
    static double out(double t) {
        double s = 1 - clamp01(t);
        return 1 - s * s;
    }

    /** Starts faster and settles longer than {@link #out}: a blast or a burst. */
    static double outStrong(double t) {
        double s = 1 - clamp01(t);
        return 1 - s * s * s;
    }

    /** Starts and ends slowly: what is moved on purpose. */
    static double inOut(double t) {
        double s = clamp01(t);
        return s * s * (3 - 2 * s);
    }

    /** 0 below {@code from}, 1 above {@code to}, {@link #inOut} between. */
    static double inOut(double from, double to, double t) {
        return inOut((t - from) / (to - from));
    }
}
