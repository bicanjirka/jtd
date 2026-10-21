package td.tower.cinder;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/** Lingering Flames: each wave leaves a patch of burning ground where its target stands, for two seconds. */
public final class LingeringFlamesPerk implements CinderPerk {

    private static final float RADIUS_CELLS = 0.7f;
    private static final int TICKS = 40;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withLinger(LingerSpec.of(RADIUS_CELLS, TICKS));
    }

    @Override
    public TermText describe() {
        return TermText.of("each wave leaves flames where its target stands for "
                + TermNumbers.seconds(TICKS / (float) TickRate.TICKS_PER_SECOND) + " that apply ").plus(EffectKind.BURN);
    }
}
