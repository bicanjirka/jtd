package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Broadcast: Kinship counts tower types a cell further and loses its cap. */
public final class FarKinshipPerk implements AuraPerk {

    private static final float EXTRA_REACH_CELLS = 1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(spec.kinship().reachingFurther(EXTRA_REACH_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.KINSHIP)
                .plus(" counts tower types " + TermNumbers.cells(EXTRA_REACH_CELLS) + " further and loses its cap");
    }
}
