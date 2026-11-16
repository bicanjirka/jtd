package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;
import td.tower.TowerTerm;

/**
 * The mark a kind of tower leaves, as the light's Prism carries it: one stack, or what one stack is for that mark.
 * Declared in the order the Beacon's pane lists them.
 */
public enum PrismMark {
    SUNDER(EffectKind.SUNDERED, 1),
    CRACK(EffectKind.CRACKED, 1),
    // One stack of Scorch is a single point of resilience; what the Scorcher's sear gives is worth carrying.
    SCORCH(EffectKind.SCORCHED, 5),
    CHILL(EffectKind.CHILL, 1),
    TOLL(EffectKind.TOLL, 1),
    STATIC(TowerTerm.STATIC, 1),
    SOULBRAND(TowerTerm.SOULBRAND, 1);

    /** The slow the light's Chill takes. */
    public static final float CHILL_STRENGTH = 0.15f;

    private final Term term;
    private final int stacks;

    PrismMark(Term term, int stacks) {
        this.term = term;
        this.stacks = stacks;
    }

    /** What the mark is called: the effect, or the Burst passive whose stack it is. */
    public Term term() {
        return this.term;
    }

    /** The stacks the light adds as an enemy enters it. */
    public int stacks() {
        return this.stacks;
    }
}
