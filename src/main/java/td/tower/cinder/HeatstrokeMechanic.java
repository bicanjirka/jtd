package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/**
 * Heatstroke: each second of the stream, starting with its first tick, an enemy in the cone whose burn holds enough fuel
 * is dazed for a moment.
 */
public final class HeatstrokeMechanic implements CinderMechanic {

    /** The fuel, in points, a burn holds from which it flashes over. */
    public static final float HOT_FUEL_POINTS = 80f;
    public static final float DAZE_SECONDS = 0.5f;
    public static final int DAZE_TICKS = Math.round(DAZE_SECONDS * TickRate.TICKS_PER_SECOND);

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withHeatstroke();
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED);
    }

    @Override
    public TermText describe() {
        return TermText.of("each second of the stream, an enemy in the cone whose ").plus(EffectKind.BURN)
                .plus(" holds " + TermNumbers.whole(HOT_FUEL_POINTS) + " fuel or more gets ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(DAZE_SECONDS));
    }
}
