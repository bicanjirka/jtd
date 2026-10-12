package td.tower;

/** A yes or no a node or a status gives a tower. */
public enum TowerCapability {
    GUARANTEED_CRIT("Guaranteed crit"),
    MAGIC_DAMAGE("Deals magic damage"),
    CRITS_PIERCE_SHIELDS("Crits pierce shields");

    private final String label;

    TowerCapability(String label) {
        this.label = label;
    }

    /** How it reads as a row of its own. */
    public String label() {
        return this.label;
    }
}
