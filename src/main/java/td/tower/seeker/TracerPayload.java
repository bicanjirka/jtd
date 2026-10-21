package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.Set;

/** Tracer: the missile reveals its target and marks it, for four seconds. */
public final class TracerPayload implements Payload {

    private static final float SPOT_SECONDS = 4f;

    @Override
    public MissileLook look() {
        return MissileLook.TRACER;
    }

    @Override
    public TowerTerm term() {
        return TowerTerm.TRACER_MISSILE;
    }

    @Override
    public Set<EffectKind> applies() {
        return Set.of(EffectKind.REVEALED, EffectKind.MARKED);
    }

    @Override
    public String label() {
        return "tracer";
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.spot(target, Math.round(SPOT_SECONDS * strength * TickRate.TICKS_PER_SECOND));
    }

    @Override
    public TermText effect() {
        return TermText.of("applies ").plus(EffectKind.REVEALED).plus(" and ").plus(EffectKind.MARKED)
                .plus(" to its target for " + TermNumbers.seconds(SPOT_SECONDS));
    }
}
