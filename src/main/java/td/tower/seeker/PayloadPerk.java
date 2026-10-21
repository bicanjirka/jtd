package td.tower.seeker;

import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;

import java.util.Set;

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

    @Override
    public Set<Term> applies() {
        return Set.copyOf(this.payload.applies());
    }

    @Override
    public TermText describe() {
        return TermText.of("every " + TermNumbers.ordinal(PayloadPlan.EVERY)
                + " missile carries a payload, the payloads taking turns; adds the ")
                .plus(TermText.of(this.payload.term(), this.payload.effect()));
    }
}
