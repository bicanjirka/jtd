package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;

/**
 * The mark a kind of tower leaves, as the light's Prism carries it: one stack, or what one stack is for that mark.
 * Declared in the order the Beacon's pane lists them.
 */
public enum PrismMark {
    SUNDER(EffectKind.SUNDERED, 1),
    CRACK(EffectKind.CRACKED, 1),
    UNRAVEL(EffectKind.UNRAVELED, 1),
    SINGE(EffectKind.SINGED, 1),
    VULNERABLE(EffectKind.VULNERABLE, 1),
    TOLL(EffectKind.TOLL, 1);

    private final Term term;
    private final int stacks;

    PrismMark(Term term, int stacks) {
        this.term = term;
        this.stacks = stacks;
    }

    /** What the mark is called: the effect it puts on. */
    public Term term() {
        return this.term;
    }

    /** The stacks the light adds as an enemy enters it. */
    public int stacks() {
        return this.stacks;
    }
}
