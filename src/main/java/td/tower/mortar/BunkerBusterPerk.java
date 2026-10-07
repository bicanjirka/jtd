package td.tower.mortar;

/** Bunker Buster: the enemy at the centre takes triple and is Sundered, and the blast shrinks by 30%. */
public final class BunkerBusterPerk implements MortarPerk {

    private static final float DAMAGE_FACTOR = 3f;
    private static final int SUNDER_STACKS = 3;
    private static final float CENTRE_RADIUS_CELLS = 0.5f;
    private static final float BLAST_FACTOR = 0.7f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withCentre(CentreSpec.of(DAMAGE_FACTOR, SUNDER_STACKS, CENTRE_RADIUS_CELLS))
                .withBlastScaledBy(BLAST_FACTOR);
    }
}
