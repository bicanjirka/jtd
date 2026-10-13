package td.tower.cinder;

/** Soulfire: every wave also burns as Soulfire, a blue burn in a pool of its own beside the ordinary one. */
public final class SoulfirePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSoulfire();
    }

    @Override
    public String describe() {
        return "every wave also burns as Soulfire: a blue burn in its own pool that doubles the ordinary burn, earns Sickened as "
                + "well as Scorched and burns the burn-immune; Stoke applies to both";
    }
}
