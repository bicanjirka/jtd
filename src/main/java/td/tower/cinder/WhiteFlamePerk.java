package td.tower.cinder;

/** White Flame: the flame burns white. */
public final class WhiteFlamePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withLook(FlameLook.WHITE);
    }

    @Override
    public String describe() {
        return "the flame burns white";
    }
}
