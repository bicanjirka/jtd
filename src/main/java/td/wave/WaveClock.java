package td.wave;

/**
 * Where the game is in its waves: how many have started, whether one is out, and the tick the last one started on.
 *
 * @param started   how many waves have started
 * @param out       whether a wave is out: from its start until its clear, or until the board is cleared of it
 * @param startTick the tick the last wave started on
 */
public record WaveClock(long started, boolean out, int startTick) {

    private static final WaveClock FIRST = new WaveClock(0, false, 0);

    /** Before any wave. */
    static WaveClock first() {
        return FIRST;
    }

    /** A wave starts on tick {@code gameTime}. */
    WaveClock start(int gameTime) {
        return new WaveClock(this.started + 1, true, gameTime);
    }

    /** The wave out is over. */
    WaveClock end() {
        return this.out ? new WaveClock(this.started, false, this.startTick) : this;
    }

    /** A level starts over: no wave out and the clock from zero; the count goes on, so no old stamp matches. */
    WaveClock startOver() {
        return new WaveClock(this.started, false, 0);
    }
}
