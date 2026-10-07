package td.tower.pulse;

/** Meltdown: Toll stacks to ten, the zap crits more often, and Toll raises the zap's damage too. */
public final class MeltdownPerk implements PulsePerk {

    private static final int TOLL_CAP = 10;
    private static final float CRIT_BONUS = 0.1f;

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withToll(spec.toll().withCap(TOLL_CAP))
                .withZap(spec.zap().withCritBonus(CRIT_BONUS).withTollRaising());
    }
}
