package td.util;

/**
 * Thread-ownership assertions, so the ownership rules in CLAUDE.md §3 fail loudly at the
 * point of violation instead of being a comment nobody can check.
 * <p>
 * Identification is by thread <em>name</em> rather than by an AWT or Swing call, because
 * {@code td.util} is one of the headless packages (CLAUDE.md 2.1) and must not import
 * either. The game loop names its thread {@code game-loop} ({@code GameLoop.start}); the
 * Event Dispatch Thread is named {@code AWT-EventQueue-N} by the JDK.
 * <p>
 * The checks are always on. Each is a string comparison against the current thread's name,
 * which is cheap enough for lifecycle methods and per-tick entry points alike; they are
 * deliberately not gated behind a system property, since a check that is off in the build
 * the player runs is a check that never fires where it matters.
 */
public final class Threads {

    /**
     * The name {@code GameLoop.start} gives the simulation thread.
     */
    public static final String GAME_LOOP = "game-loop";

    /**
     * The prefix the JDK gives every Event Dispatch Thread.
     */
    private static final String EDT_PREFIX = "AWT-EventQueue";

    private Threads() {
    }

    /**
     * Whether the calling thread is the one the simulation runs on.
     */
    public static boolean onGameLoop() {
        return GAME_LOOP.equals(Thread.currentThread().getName());
    }

    /**
     * Whether the calling thread is Swing's Event Dispatch Thread.
     */
    public static boolean onEventDispatchThread() {
        return Thread.currentThread().getName().startsWith(EDT_PREFIX);
    }

    /**
     * Fails unless the caller is on the game-loop thread.
     *
     * @param what the operation being guarded, named as it appears in the error
     * @throws IllegalStateException if called from any other thread
     */
    public static void assertGameLoop(String what) {
        if (!onGameLoop()) {
            throw new IllegalStateException(
                    what + " must run on the " + GAME_LOOP + " thread, but ran on " + current());
        }
    }

    /**
     * Fails unless the caller is on Swing's Event Dispatch Thread. Guards the EDT-owned side
     * of the boundary: building the component tree, and the level lifecycle that swaps the
     * state the loop thread reads.
     *
     * @param what the operation being guarded, named as it appears in the error
     * @throws IllegalStateException if called from any other thread
     */
    public static void assertEventDispatchThread(String what) {
        if (!onEventDispatchThread()) {
            throw new IllegalStateException(
                    what + " must run on the Event Dispatch Thread, but ran on " + current());
        }
    }

    /**
     * Fails if the caller is on the Event Dispatch Thread. Used to guard operations that
     * block on the game-loop thread, which would otherwise freeze the UI.
     *
     * @param what the operation being guarded, named as it appears in the error
     * @throws IllegalStateException if called from the EDT
     */
    public static void assertNotEventDispatchThread(String what) {
        if (onEventDispatchThread()) {
            throw new IllegalStateException(
                    what + " blocks on the " + GAME_LOOP + " thread and must not run on the EDT");
        }
    }

    private static String current() {
        return Thread.currentThread().getName();
    }
}
