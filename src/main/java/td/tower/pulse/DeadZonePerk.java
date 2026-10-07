package td.tower.pulse;

/** Dead Zone: nothing inside the field can be healed or shielded. */
public final class DeadZonePerk implements PulsePerk {

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withModes(spec.modes().withDeadZone());
    }
}
