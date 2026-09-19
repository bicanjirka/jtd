package td.enemy;

/**
 * The closed, ordered difficulty ladder that replaces the old numeric wave {@code level} -
 * every enemy, wave and kill is expressed in one of these five named tiers, never a plain
 * number, anywhere a wave author or the player can see it. Declaration order <em>is</em> the
 * ladder order ({@link #values()}/{@link #compareTo}), which is what lets {@link RankedEnemy}
 * define "the enemy's own highest defined rank" without a separate ordering mechanism.
 */
public enum Rank {

    GRUNT(1.0f),
    SOLDIER(1.3f),
    VETERAN(1.7f),
    ELITE(2.2f),
    BOSS(3.0f);

    private final float scoreMultiplier;

    Rank(float scoreMultiplier) {
        this.scoreMultiplier = scoreMultiplier;
    }

    /**
     * How much more a kill at this rank is worth in score than in bounty - see
     * {@code EconomyDelta#kill(int, int)}. A starting curve, easy to retune later since every
     * rank's weight lives here, in one place.
     */
    public float scoreMultiplier() {
        return this.scoreMultiplier;
    }
}
