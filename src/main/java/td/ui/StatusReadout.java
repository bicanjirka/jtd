package td.ui;

import td.economy.StatusTerm;

import java.util.OptionalInt;

/**
 * What the status bar shows at one moment: the round, the level's rounds and whether the next may start, and the
 * economy's lives, score and cash.
 *
 * @param round     rounds started so far
 * @param rounds    rounds in the level
 * @param waveReady whether the next round may be sent now
 * @param levelOver whether the level has been won or lost
 */
record StatusReadout(int round, int rounds, boolean waveReady, boolean levelOver, int lives, int score,
                     int credits) {

    /** Up to this many lives the bar shows a heart each; more, or none, it shows the number. */
    static final int MAX_HEARTS = 10;

    /** Before any level: nothing counted. */
    static StatusReadout empty() {
        return new StatusReadout(0, 0, false, false, 0, 0, 0);
    }

    /** What a tooltip and a number beside an icon say: {@code "7/18"}, {@code "5"}, {@code "1240"}, {@code "4964"}. */
    String value(StatusTerm term) {
        return switch (term) {
            case WAVE -> this.round + "/" + this.rounds;
            case LIVES -> Integer.toString(this.lives);
            case SCORE -> Integer.toString(this.score);
            case CASH -> Integer.toString(this.credits);
        };
    }

    /** How many hearts show the lives, when there are few enough to count; empty when the number is shown instead. */
    OptionalInt hearts() {
        return this.lives >= 1 && this.lives <= MAX_HEARTS ? OptionalInt.of(this.lives) : OptionalInt.empty();
    }

    /** The share of the level's rounds started, from 0 to 1; 0 before a level. */
    float waveShare() {
        return this.rounds <= 0 ? 0f : Math.clamp((float) this.round / this.rounds, 0f, 1f);
    }
}
