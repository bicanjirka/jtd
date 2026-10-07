package td.tower.pulse;

/** Overcharged Coils: Toll builds twice as fast. */
public final class FasterTollPerk implements PulsePerk {

    private static final float FACTOR = 2f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withToll(spec.toll().buildingFasterBy(FACTOR));
    }
}
