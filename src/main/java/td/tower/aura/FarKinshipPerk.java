package td.tower.aura;

import td.term.TermText;
import td.tower.upgrade.PerkText;

/** Broadcast: Kinship counts tower types a cell further and loses its cap. */
public final class FarKinshipPerk implements AuraPerk {

    private static final float EXTRA_REACH_CELLS = 1f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(spec.kinship().reachingFurther(EXTRA_REACH_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of("Kinship counts tower types " + PerkText.distance(EXTRA_REACH_CELLS)
                + " further and loses its cap");
    }
}
