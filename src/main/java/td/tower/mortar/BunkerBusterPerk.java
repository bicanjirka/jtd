package td.tower.mortar;

/** Bunker Buster: the enemy at the centre takes triple and is Sundered. */
public final class BunkerBusterPerk implements MortarPerk {

    private static final float DAMAGE_FACTOR = 3f;
    private static final int SUNDER_STACKS = 3;
    private static final float CENTRE_RADIUS_CELLS = 0.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withCentre(CentreSpec.of(DAMAGE_FACTOR, SUNDER_STACKS, CENTRE_RADIUS_CELLS));
    }
}
