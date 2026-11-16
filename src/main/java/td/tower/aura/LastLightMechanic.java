package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Last Light: the beam holds the enemy about to leak. */
public final class LastLightMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withSpotlight(Spotlight.LAST_LIGHT);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.PRIORITY);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.SPOTLIGHT).plus(": the beam holds the enemy in the light nearest its exit; it has ")
                .plus(EffectKind.PRIORITY).plus(" while held");
    }
}
