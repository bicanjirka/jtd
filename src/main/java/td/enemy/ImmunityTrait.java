package td.enemy;

import td.effect.EffectKind;
import td.term.TermText;

/** {@code kind} never takes hold on this enemy. */
public record ImmunityTrait(EffectKind kind) implements Trait {

    @Override
    public boolean keepsOut(EffectKind incoming) {
        return incoming == this.kind;
    }

    @Override
    public TraitEntry entry() {
        return TraitEntry.immunity(this.kind, TermText.of("immune to ").plus(this.kind));
    }
}
