package td.tower.mortar;

/**
 * Bracketing: a shell that lands within {@code radiusCells} of the last one hits harder and wider,
 * one step more for each shell in a row, up to {@code maxSteps}; a shell elsewhere starts over.
 *
 * @param active      whether the Mortar brackets at all
 * @param radiusCells how close to the last landing counts as bracketed
 * @param maxSteps    how many steps a run of shells can climb
 * @param damageStep  the damage each step adds, as a fraction
 * @param radiusStep  the blast radius each step adds, as a fraction
 */
public record BracketSpec(boolean active, float radiusCells, int maxSteps, float damageStep, float radiusStep) {

    /** A Mortar that is not attuned does not bracket. */
    public static BracketSpec none() {
        return new BracketSpec(false, 0f, 0, 0f, 0f);
    }

    public static BracketSpec of(float radiusCells, int maxSteps, float damageStep, float radiusStep) {
        return new BracketSpec(true, radiusCells, maxSteps, damageStep, radiusStep);
    }

    public BracketSpec withDamageStep(float damageStep) {
        return new BracketSpec(this.active, this.radiusCells, this.maxSteps, damageStep, this.radiusStep);
    }
}
