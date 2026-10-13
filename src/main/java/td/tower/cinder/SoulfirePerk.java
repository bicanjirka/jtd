package td.tower.cinder;

/** Soulfire: every other wave is Soulfire, a blue burn in a pool of its own. */
public final class SoulfirePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSoulfire();
    }

    @Override
    public String describe() {
        return "every other wave is Soulfire: a blue burn in its own pool that stacks with ordinary burn, earns Sickened as well "
                + "as Scorched and burns the burn-immune; Stoke applies to both";
    }
}
