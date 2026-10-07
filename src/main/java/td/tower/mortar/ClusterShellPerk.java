package td.tower.mortar;

/** Cluster Shell: four bomblets scattered along the path around the impact, each a small blast for 40%. */
public final class ClusterShellPerk implements MortarPerk {

    private static final int COUNT = 4;
    private static final float DAMAGE_SHARE = 0.4f;
    private static final float RADIUS_CELLS = 1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBomblets(BombletSpec.of(COUNT, false, DAMAGE_SHARE, RADIUS_CELLS));
    }
}
