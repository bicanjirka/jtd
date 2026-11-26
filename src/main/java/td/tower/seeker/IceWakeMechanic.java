package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Ice Wake: an enemy the Aerie froze Chills the enemies walking close past it while the freeze lasts. */
public final class IceWakeMechanic implements SeekerMechanic {

    public static final float RADIUS_CELLS = 1.0f;
    public static final float CHILL = 0.5f;
    /** How long the Chill holds once an enemy walks out of the wake. */
    public static final int CHILL_TICKS = 10;

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withIce(IceRider.ICE_WAKE);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.CHILL);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy it froze leaves an ").plus(TowerTerm.ICE_WAKE).plus(": every enemy within "
                + TermNumbers.cells(RADIUS_CELLS) + " of it is ").plus(EffectKind.CHILL)
                .plus(" by " + TermNumbers.percent(CHILL) + " while the freeze lasts");
    }
}
