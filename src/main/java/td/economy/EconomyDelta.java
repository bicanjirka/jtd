package td.economy;

/**
 * What one game event changes about the economy. {@link #none()} is the identity, so deltas fold
 * with {@code reduce(EconomyDelta::plus)}.
 */
public record EconomyDelta(int credits, int score, int lives) {

    private static final EconomyDelta NONE = new EconomyDelta(0, 0, 0);

    public static EconomyDelta none() {
        return NONE;
    }

    public static EconomyDelta credits(int amount) {
        return new EconomyDelta(amount, 0, 0);
    }

    public static EconomyDelta score(int amount) {
        return new EconomyDelta(0, amount, 0);
    }

    public static EconomyDelta lives(int amount) {
        return new EconomyDelta(0, 0, amount);
    }

    /** A kill whose score equals its bounty. */
    public static EconomyDelta kill(int bounty) {
        return new EconomyDelta(bounty, bounty, 0);
    }

    /** A kill whose score is weighted separately from its bounty. */
    public static EconomyDelta kill(int bounty, int score) {
        return new EconomyDelta(bounty, score, 0);
    }

    /** An enemy reaching the end of the path: costs a life and score. */
    public static EconomyDelta leak(int penalty) {
        return new EconomyDelta(0, -penalty, -1);
    }

    public EconomyDelta plus(EconomyDelta other) {
        return new EconomyDelta(
                this.credits + other.credits,
                this.score + other.score,
                this.lives + other.lives);
    }
}
