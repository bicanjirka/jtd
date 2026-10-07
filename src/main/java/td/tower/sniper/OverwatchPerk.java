package td.tower.sniper;

/**
 * Overwatch: the Sniper can't shoot what is this close. The distance is fixed: range bonuses don't
 * move it.
 */
public final class OverwatchPerk implements SniperPerk {

    private static final float DEAD_ZONE_CELLS = 2f;

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.withReach(spec.reach().withDeadZone(DEAD_ZONE_CELLS * spec.view().cellSize()));
    }
}
