package td.tower.cinder;

/** Everburn: a pool never decays below a quarter of its strongest application while its enemy is in range. */
public final class EverburnPerk implements CinderPerk {

    private static final float FLOOR = 0.25f;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withEverburnFloor(FLOOR);
    }
}
