package td.tower.mortar;

/** Tactical Nuke: every fourth shell is a nuke: four times the damage over one and a half times the blast. */
public final class TacticalNukePerk implements MortarPerk {

    private static final int EVERY = 4;
    private static final float DAMAGE_FACTOR = 4f;
    private static final float RADIUS_FACTOR = 1.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withNuke(NukeSpec.of(EVERY, DAMAGE_FACTOR, RADIUS_FACTOR))
                .withShells(spec.shells().withNukeEvery(EVERY));
    }
}
