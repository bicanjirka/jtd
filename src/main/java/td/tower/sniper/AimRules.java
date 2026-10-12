package td.tower.sniper;

/**
 * How Steady Aim builds beyond its stack cap, which is a tower stat: whether the stacks outlive the
 * enemy that earned them. Perks refine it.
 *
 * @param survivesKill whether the stacks carry over when the target died
 */
public record AimRules(boolean survivesKill) {

    /** Attune's Steady Aim: lost with its target. */
    public static AimRules attuned() {
        return new AimRules(false);
    }

    public AimRules thatSurvivesAKill() {
        return new AimRules(true);
    }
}
