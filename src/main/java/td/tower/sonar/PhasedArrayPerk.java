package td.tower.sonar;

/** Phased Array: the beam stops spinning and stays on the enemy with the most health. */
public final class PhasedArrayPerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withBeam(spec.beam().thatIsPhased());
    }

    @Override
    public String describe() {
        return "stops spinning and holds the beam on the enemy with most health, hitting everything on it "
                + PhasedBeam.HITS_PER_REVOLUTION + " times a revolution";
    }
}
