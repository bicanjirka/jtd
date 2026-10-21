package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Cluster Shell: four bomblets scattered along the path around the impact, each a small blast for 40%. */
public final class ClusterShellMechanic implements MortarMechanic {

    private static final int COUNT = 4;
    private static final float DAMAGE_SHARE = 0.4f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBomblets(BombletSpec.of(COUNT, false, DAMAGE_SHARE, RADIUS_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of("the shell breaks into " + COUNT + " bombs along the path around the impact, each a ")
                .plus(TowerTerm.BOMBLET).plus(" of " + TermNumbers.percent(DAMAGE_SHARE) + " damage over "
                        + TermNumbers.cells(RADIUS_CELLS));
    }
}
