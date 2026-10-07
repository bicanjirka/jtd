package td.tower.mortar;

/** A special that unlocks a shell type: the Mortar fires it every third shell. */
public final class ShellTypePerk implements MortarPerk {

    private final ShellType type;

    public ShellTypePerk(ShellType type) {
        this.type = type;
    }

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShells(spec.shells().withSpecial(this.type));
    }
}
