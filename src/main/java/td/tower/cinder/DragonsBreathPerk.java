package td.tower.cinder;

/** Dragon's Breath: a continuous stream that keeps every enemy's Stoke full. */
public final class DragonsBreathPerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withStoke(spec.stoke().full());
    }
}
