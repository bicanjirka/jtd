package td.tower.cinder;

/** Dragon's Breath: a continuous stream that keeps every enemy's Stoke full. */
public final class DragonsBreathPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withStoke(spec.stoke().full());
    }

    @Override
    public String describe() {
        return "a continuous stream that keeps every pool topped up and Stoke always full";
    }
}
