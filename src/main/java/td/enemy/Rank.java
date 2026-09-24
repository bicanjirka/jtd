package td.enemy;

/**
 * The ordered difficulty ladder every enemy, wave and kill is expressed in. Declaration order is
 * ladder order.
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

    /** How much more a kill at this rank scores than it pays. */
    public float scoreMultiplier() {
        return this.scoreMultiplier;
    }
}
