package td.tower.sniper;

/**
 * How Steady Aim builds: how many stacks it may hold, and whether they outlive the enemy that earned
 * them. Perks refine it.
 *
 * @param stackCap     the most stacks Steady Aim holds
 * @param survivesKill whether the stacks carry over when the target died
 */
public record AimRules(int stackCap, boolean survivesKill) {

    /** Attune's Steady Aim: one stack, lost with its target. */
    public static AimRules attuned() {
        return new AimRules(1, false);
    }

    public AimRules withStackCap(int stackCap) {
        return new AimRules(stackCap, this.survivesKill);
    }

    public AimRules thatSurvivesAKill() {
        return new AimRules(this.stackCap, true);
    }
}
