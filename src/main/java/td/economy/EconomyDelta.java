package td.economy;

/**
 * What a single game event changes about the player's economy. {@link #none()} is the
 * identity element - {@code none().plus(x)} and {@code x.plus(none())} both equal
 * {@code x} - so a stream of deltas can be folded with {@code reduce(EconomyDelta::plus)}
 * with no null checks or size-0/size-1 special cases.
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

    /**
     * An enemy killed for its bounty: credits the player and scores the same amount. Every test
     * that doesn't care about a kill's rank-weighted score uses this 1:1 shorthand.
     */
    public static EconomyDelta kill(int bounty) {
        return new EconomyDelta(bounty, bounty, 0);
    }

    /**
     * An enemy killed for its bounty, with a score decoupled from it - {@code
     * AbstractEnemyMob.doDamage}'s real kill path, where score is weighted by the killed mob's
     * {@code Rank} rather than mirroring its bounty 1:1.
     */
    public static EconomyDelta kill(int bounty, int score) {
        return new EconomyDelta(bounty, score, 0);
    }

    /**
     * An enemy that reached the end of the path: costs a life and deducts score.
     */
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
