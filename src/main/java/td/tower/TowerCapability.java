package td.tower;

import td.effect.EffectKind;
import td.term.TermText;

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

    /** What it does, as a node's sentence. */
    public TermText describe() {
        return switch (this) {
            case GUARANTEED_CRIT -> TermText.of("every hit is a crit");
            case MAGIC_DAMAGE -> TermText.of("deals magic damage");
            case CRITS_PIERCE_SHIELDS -> TermText.format("a crit passes through {}", EffectKind.SHIELD);
        };
    }
}
