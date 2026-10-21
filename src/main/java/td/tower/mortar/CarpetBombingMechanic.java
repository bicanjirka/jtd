package td.tower.mortar;

import td.term.TermText;
import td.tower.TowerTerm;

/** Carpet Bombing: eight bomblets in a line along the path ahead of the impact. */
public final class CarpetBombingMechanic implements MortarMechanic {

    private static final int COUNT = 8;
    private static final float DAMAGE_SHARE = 0.4f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBomblets(BombletSpec.of(COUNT, true, DAMAGE_SHARE, RADIUS_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of(COUNT + " bombs instead, each a ").plus(TowerTerm.BOMBLET)
                .plus(", in a line along the path ahead of the impact");
    }
}
