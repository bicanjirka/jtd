package td.tower.mortar;

/** Fragmentation Rounds II: the shrapnel does 30% more of everything and carries its shell's own effect. */
public final class ShrapnelBoostPerk implements MortarPerk {

    private static final float FACTOR = 1.3f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(spec.shrapnel().scaledBy(FACTOR).carryingTheShellsEffect());
    }
}
