package td.tower.seeker;

/** The last Mixed Payloads node: every missile carries a payload, cycling through all four, each stronger. */
public final class FullRackPerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withPayloads(spec.payloads().forEveryMissile());
    }
}
