package td.tower.pulse;

/** Attune's Toll: each second an enemy spends in the field adds a stack, up to five. */
public final class TollPerk implements PulsePerk {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withToll(TollSpec.attuned());
    }
}
