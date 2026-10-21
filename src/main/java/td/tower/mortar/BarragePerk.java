package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/** Barrage: the reload takes twice as long, but every salvo is three shells half a second apart. */
public final class BarragePerk implements MortarPerk {

    private static final int SHELLS = 3;
    private static final int GAP_TICKS = 10;
    private static final float RELOAD_FACTOR = 2f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withSalvo(SalvoSpec.of(SHELLS, GAP_TICKS, RELOAD_FACTOR));
    }

    @Override
    public TermText describe() {
        return TermText.of("the reload takes " + TermNumbers.times(RELOAD_FACTOR) + " as long, but each salvo is " + SHELLS
                + " shells " + TermNumbers.seconds(GAP_TICKS / (float) TickRate.TICKS_PER_SECOND)
                + " apart at full damage");
    }
}
