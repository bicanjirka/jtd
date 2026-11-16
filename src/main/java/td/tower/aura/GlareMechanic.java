package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Glare: nothing in the light dodges as well, and crits find it. */
public final class GlareMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withEdge(LightSpec.Edge.GLARE);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.EXPOSED);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy in the light is ").plus(EffectKind.EXPOSED);
    }
}
