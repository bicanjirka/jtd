package td.enemy;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/** {@code kind} lasts only {@code share} of its time on this enemy. */
public record ShortenedEffectTrait(EffectKind kind, float share) implements Trait {

    public ShortenedEffectTrait {
        if (share <= 0f || share >= 1f) {
            throw new IllegalArgumentException("A shortened effect keeps a share between 0 and 1, was " + share);
        }
    }

    @Override
    public float durationShare(EffectKind incoming) {
        return incoming == this.kind ? this.share : 1f;
    }

    @Override
    public TraitEntry entry() {
        return TraitEntry.of(this.kind, TermText.format("a {} on it lasts " + TermNumbers.percent(this.share)
                + " as long", this.kind));
    }
}
