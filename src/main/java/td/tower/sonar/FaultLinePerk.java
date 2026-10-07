package td.tower.sonar;

/** Fault Line: the resilience Resonant Crack takes falls further and doesn't come back while Exposed. */
public final class FaultLinePerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withFaultLine();
    }
}
