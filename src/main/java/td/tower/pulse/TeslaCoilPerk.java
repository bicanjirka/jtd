package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Tesla Coil: the zap chains to three more enemies, even outside the field, and Dazes every one it hits. */
public final class TeslaCoilPerk implements PulsePerk {

    private static final int CHAINS = 3;
    private static final float CHAIN_CELLS = 1.5f;
    private static final float DAZE_SECONDS = 0.25f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withZap(spec.zap().chainingTo(CHAINS, CHAIN_CELLS).dazing(DAZE_SECONDS));
    }

    @Override
    public String describe() {
        return "the zap chains to " + CHAINS + " more enemies within " + PerkText.cells(CHAIN_CELLS)
                + " cells, even outside the field; every enemy zapped is Dazed "
                + PerkText.seconds(Math.round(DAZE_SECONDS * TickRate.TICKS_PER_SECOND));
    }
}
