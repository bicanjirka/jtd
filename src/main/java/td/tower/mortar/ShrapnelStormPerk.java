package td.tower.mortar;

/** Shrapnel Storm: what the shrapnel hits bleeds. */
public final class ShrapnelStormPerk implements MortarPerk {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(spec.shrapnel().bleeding());
    }

    @Override
    public String describe() {
        return "what the shrapnel hits bleeds: physical damage for every cell it travels";
    }
}
