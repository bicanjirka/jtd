package td.tower.cinder;

import td.term.TermText;

/** White Flame: the flame's core glows white, in either kind of fire. */
public final class WhiteFlameMechanic implements CinderMechanic {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withLook(FlameLook.WHITE);
    }

    @Override
    public TermText describe() {
        return TermText.of("the flame's core glows white");
    }
}
