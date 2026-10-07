package td.tower.cinder;

/** Searing Flame: an ignition makes the enemy Vulnerable, and so does every wave that hits a burning enemy, once a second at most. */
public final class SearingFlamePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSearing().withLook(FlameLook.SEARING);
    }
}
