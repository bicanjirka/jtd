package td.tower.aura;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Ringleader: the beam holds the enemy that leads the others. */
public final class RingleaderMechanic implements AuraMechanic {

    @Override
    public LightSpec refineSpec(LightSpec spec) {
        return spec.withSpotlight(Spotlight.RINGLEADER);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.PRIORITY);
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.SPOTLIGHT).plus(": the beam holds an enemy in the light that gives its allies an "
                + "aura, or else the one with the most allies within " + TermNumbers.cells(Spotlight.PACK_CELLS)
                + "; it has ").plus(EffectKind.PRIORITY).plus(" while held");
    }
}
