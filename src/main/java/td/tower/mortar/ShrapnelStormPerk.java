package td.tower.mortar;

/** Shrapnel Storm: what the shrapnel hits bleeds. */
public final class ShrapnelStormPerk implements MortarPerk {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(spec.shrapnel().bleeding());
    }
}
