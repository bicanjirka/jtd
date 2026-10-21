package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.tower.upgrade.PerkText;

import java.util.Set;

/** Searing Flame: an ignition makes the enemy Vulnerable, and so does every wave that hits a burning enemy, once a second at most. */
public final class SearingFlamePerk implements CinderPerk {

    private static final int INTERVAL_TICKS = 20;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSearing(INTERVAL_TICKS).withLook(FlameLook.SEARING);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.VULNERABLE);
    }

    @Override
    public String describe() {
        return "an ignition grants Vulnerable, and so does every wave that hits a burning enemy, at most one stack every "
                + PerkText.seconds(INTERVAL_TICKS);
    }
}
