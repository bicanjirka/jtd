package td.economy;

/**
 * The player's credits/score/lives as one immutable snapshot, replacing three separately
 * mutated {@code int} fields with a single value that moves by applying an
 * {@link EconomyDelta}.
 */
public record EconomyState(int credits, int score, int lives) {

    public static EconomyState startingWith(int credits, int lives) {
        return new EconomyState(credits, 0, lives);
    }

    public EconomyState after(EconomyDelta delta) {
        return new EconomyState(
                this.credits + delta.credits(),
                this.score + delta.score(),
                this.lives + delta.lives());
    }

    public boolean canAfford(int amount) {
        return this.credits >= amount;
    }

    public boolean isGameOver() {
        return this.lives <= 0;
    }
}
