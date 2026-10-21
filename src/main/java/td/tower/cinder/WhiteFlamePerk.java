package td.tower.cinder;

import td.term.TermText;

/** White Flame: the flame burns white. */
public final class WhiteFlamePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withLook(FlameLook.WHITE);
    }

    @Override
    public TermText describe() {
        return TermText.of("the flame turns white");
    }
}
