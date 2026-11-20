package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

/** Lasting Mix: a payload shell's ground lasts longer. */
public final class LastingMixMechanic implements MortarMechanic {

    private static final int EXTRA_TICKS = Math.round(TickRate.TICKS_PER_SECOND * 3f);

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withPayload(spec.payload().longer(EXTRA_TICKS));
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(TowerTerm.PAYLOAD).plus(" shell's ground lasts "
                + TermNumbers.seconds(EXTRA_TICKS / TickRate.TICKS_PER_SECOND) + " longer");
    }
}
