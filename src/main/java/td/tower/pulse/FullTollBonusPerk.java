package td.tower.pulse;

/** Overcharged Coils II: an enemy at full Toll takes a quarter more from the field. */
public final class FullTollBonusPerk implements PulsePerk {

    private static final float BONUS = 0.25f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withField(spec.field().withFullTollBonus(BONUS));
    }
}
