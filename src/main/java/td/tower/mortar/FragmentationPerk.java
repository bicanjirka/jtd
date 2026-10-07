package td.tower.mortar;

/** Fragmentation Rounds: a ring of shrapnel past the blast for a quarter of the damage, reaching further per step. */
public final class FragmentationPerk implements MortarPerk {

    private static final float DAMAGE_SHARE = 0.25f;
    private static final float REACH_CELLS_PER_STEP = 0.25f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(ShrapnelSpec.of(DAMAGE_SHARE, REACH_CELLS_PER_STEP));
    }
}
