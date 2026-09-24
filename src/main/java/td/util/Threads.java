package td.util;

/**
 * Thread-ownership assertions that fail loudly where a rule is broken. Threads are identified by
 * name, since this package must not import AWT or Swing. Always on: a check disabled in the
 * player's build never fires where it matters.
 */
public final class Threads {

    public static final String GAME_LOOP = "game-loop";

    private static final String EDT_PREFIX = "AWT-EventQueue";

    private Threads() {
    }

    public static boolean onGameLoop() {
        return GAME_LOOP.equals(Thread.currentThread().getName());
    }

    public static boolean onEventDispatchThread() {
        return Thread.currentThread().getName().startsWith(EDT_PREFIX);
    }

    /**
     * @param what the guarded operation, as named in the error
     * @throws IllegalStateException off the game-loop thread
     */
    public static void assertGameLoop(String what) {
        if (!onGameLoop()) {
            throw new IllegalStateException(
                    what + " must run on the " + GAME_LOOP + " thread, but ran on " + current());
        }
    }

    /**
     * @param what the guarded operation, as named in the error
     * @throws IllegalStateException off the EDT
     */
    public static void assertEventDispatchThread(String what) {
        if (!onEventDispatchThread()) {
            throw new IllegalStateException(
                    what + " must run on the Event Dispatch Thread, but ran on " + current());
        }
    }

    /**
     * For operations that block on the game-loop thread and would freeze the UI.
     *
     * @param what the guarded operation, as named in the error
     * @throws IllegalStateException on the EDT
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
