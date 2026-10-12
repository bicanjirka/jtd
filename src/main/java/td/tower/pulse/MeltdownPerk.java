package td.tower.pulse;

/** Meltdown: the zap crits more often, and Toll raises the zap's damage too. */
public final class MeltdownPerk implements PulsePerk {

    private static final float CRIT_BONUS = 0.1f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withZap(spec.zap().withCritBonus(CRIT_BONUS).withTollRaising());
    }
}
