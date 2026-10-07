package td.effect;

/**
 * What an effect does to its target, for grouping. It decides which kinds diminish and how the
 * inspector labels a kind; it never changes an effect's numbers.
 */
public enum EffectCategory {
    /** Stops the target outright. */
    HARD_CC("hard CC"),
    /** Only slows the target. */
    SOFT_CC("soft CC"),
    DAMAGE_OVER_TIME("damage over time"),
    /** Changes a stat the target is hit or healed through. */
    DEBUFF("debuff"),
    /** Gives the target back health or absorbs a hit. */
    RESTORATIVE("restorative"),
    /** Marks the target for the towers that follow up on it. */
    SPOTTED("spotted"),
    /** A curse that waits for something to happen to the target, then pays its caster out. */
    HEX("hex"),
    /** Changes whether towers can see the target. */
    STEALTH("stealth");

    private final String label;

    EffectCategory(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }
}
