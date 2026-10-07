package td.tower.cinder;

/** White Flame II: hot and short: the burn lasts a quarter less. */
public final class ShortBurnPerk implements CinderPerk {

    private static final float FACTOR = 0.75f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withBurnScaledBy(FACTOR);
    }
}
