package td.tower.cinder;

import td.term.TermText;
import td.tower.TowerTerm;

/** Dragon's Breath: the long stream, with a short breath between; its numbers are the node's own. */
public final class DragonsBreathMechanic implements CinderMechanic {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withDragonsBreath();
    }

    @Override
    public TermText describe() {
        return TermText.of("its stream is ").plus(TowerTerm.DRAGONS_BREATH);
    }
}
