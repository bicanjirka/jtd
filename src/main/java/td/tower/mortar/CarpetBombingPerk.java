package td.tower.mortar;

import td.term.TermText;

/** Carpet Bombing: eight bomblets in a line along the path ahead of the impact. */
public final class CarpetBombingPerk implements MortarPerk {

    private static final int COUNT = 8;
    private static final float DAMAGE_SHARE = 0.4f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBomblets(BombletSpec.of(COUNT, true, DAMAGE_SHARE, RADIUS_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of(COUNT + " bomblets in a line along the path ahead of the impact instead");
    }
}
