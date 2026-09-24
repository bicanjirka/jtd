package td.economy;

/**
 * Credits, score and lives as one immutable snapshot, moved by applying an {@link EconomyDelta}.
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
