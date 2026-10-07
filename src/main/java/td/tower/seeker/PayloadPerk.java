package td.tower.seeker;

/** A Mixed Payloads node: every third missile may carry one more payload. */
public final class PayloadPerk implements SeekerPerk {

    private final Payload payload;

    public PayloadPerk(Payload payload) {
        this.payload = payload;
    }

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withPayloads(spec.payloads().with(this.payload));
    }
}
