package td.enemy;

/**
 * The ordered difficulty ladder every enemy, wave and kill is expressed in. Declaration order is
 * ladder order.
 */
public enum Rank {

    GRUNT(1.0f, 0),
    SOLDIER(1.3f, 1),
    VETERAN(1.7f, 2),
    // Elite climbs three bounty steps and Boss five more, so the ranks that need a plan pay for one.
    ELITE(2.2f, 5),
    BOSS(3.0f, 10);

    private final float scoreMultiplier;
    private final int bountySteps;

    Rank(float scoreMultiplier, int bountySteps) {
        this.scoreMultiplier = scoreMultiplier;
        this.bountySteps = bountySteps;
    }


    /** How much more a kill at this rank scores than it pays. */
    public float scoreMultiplier() {
        return this.scoreMultiplier;
    }

    /** How many of an enemy's bounty steps this rank adds to its grunt bounty. */
    public int bountySteps() {
        return this.bountySteps;
    }
}
