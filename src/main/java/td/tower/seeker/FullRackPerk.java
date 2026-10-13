package td.tower.seeker;

import td.tower.upgrade.PerkText;

/** The last Mixed Payloads node: every missile carries a payload, cycling through all four, each stronger. */
public final class FullRackPerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withPayloads(spec.payloads().forEveryMissile());
    }

    @Override
    public String describe() {
        return "every missile carries a payload, cycling Cryo, Arcane, EMP, Tracer, each "
                + PerkText.percent(PayloadPlan.FULL_RACK_STRENGTH - 1f) + " stronger";
    }
}
