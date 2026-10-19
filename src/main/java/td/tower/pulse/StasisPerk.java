package td.tower.pulse;

import td.tower.upgrade.PerkText;
import td.util.TickRate;

/** Stasis: an enemy reaching full Toll is Dazed, and pays for it in Toll while the Daze holds. */
public final class StasisPerk implements PulsePerk {

    private static final int STASIS_TICKS = Math.round(1f * TickRate.TICKS_PER_SECOND);
    private static final int TICKS_PER_TOLL = Math.round(0.25f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withHold(spec.hold().stilling(STASIS_TICKS, TICKS_PER_TOLL));
    }

    @Override
    public String describe() {
        return "an enemy at full Toll is Dazed for " + PerkText.seconds(STASIS_TICKS) + " and Stilled: while the Daze "
                + "holds it loses a Toll stack every " + PerkText.seconds(TICKS_PER_TOLL)
                + "; a Daze that doesn't take hold costs nothing and is tried again a second later";
    }
}
