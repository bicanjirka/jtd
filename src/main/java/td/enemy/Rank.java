package td.enemy;

/**
 * The ordered difficulty ladder every enemy, wave and kill is expressed in. Declaration order is
 * ladder order.
 */
public enum Rank {

    GRUNT(1.0f, 1.5f),
    SOLDIER(1.3f, 1f),
    VETERAN(1.7f, 0.67f),
    ELITE(2.2f, 0.5f),
    BOSS(3.0f, 0.33f);

    private final float scoreMultiplier;
    private final float freezeDrain;

    Rank(float scoreMultiplier, float freezeDrain) {
        this.scoreMultiplier = scoreMultiplier;
        this.freezeDrain = freezeDrain;
    }

    /** How fast walking wins back time frozen: seconds of it for every second walked. */
    public float freezeDrain() {
        return this.freezeDrain;
    }


    /** How much more a kill at this rank scores than it pays. */
    public float scoreMultiplier() {
        return this.scoreMultiplier;
    }
}
