package td.tower.mortar;

/** Range III's Long Battery: the dead zone grows to two and a half cells, a fixed distance range never moves. */
public final class LongBatteryPerk implements MortarPerk {

    private static final float DEAD_ZONE_CELLS = 2.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withReach(spec.reach().withDeadZone(DEAD_ZONE_CELLS * spec.view().cellSize()));
    }
}
