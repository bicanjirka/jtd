package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Searchlight: the beam holds the strongest enemy in the light. */
public final class SearchlightMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withSpotlight(Spotlight.SEARCHLIGHT);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.PRIORITY);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.SPOTLIGHT).plus(": the beam holds the enemy in the light with the highest rank, "
                + "then the most health; it has ").plus(EffectKind.PRIORITY).plus(" while held");
    }
}
