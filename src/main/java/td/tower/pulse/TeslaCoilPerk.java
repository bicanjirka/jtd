package td.tower.pulse;

/** Tesla Coil: the zap chains to three more enemies, even outside the field, and Dazes every one it hits. */
public final class TeslaCoilPerk implements PulsePerk {

    private static final int CHAINS = 3;
    private static final float CHAIN_CELLS = 1.5f;
    private static final float DAZE_SECONDS = 0.25f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withZap(spec.zap().chainingTo(CHAINS, CHAIN_CELLS).dazing(DAZE_SECONDS));
    }
}
