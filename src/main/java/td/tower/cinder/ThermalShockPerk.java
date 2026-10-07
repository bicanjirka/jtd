package td.tower.cinder;

/** Thermal Shock: freezing a burning enemy detonates its pool at 150% and chills its neighbours. */
public final class ThermalShockPerk implements CinderPerk {

    private static final float BURST_SHARE = 1.5f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withThermalShock().withTuning(spec.tuning().withFreezeBurstShare(BURST_SHARE));
    }
}
