package td.tower.seeker;

import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.util.TickRate;

/** Tracer: the missile reveals its target and marks it, for four seconds. */
public final class TracerPayload implements Payload {

    private static final float SPOT_SECONDS = 4f;

    @Override
    public MissileLook look() {
        return MissileLook.TRACER;
    }

    @Override
    public String label() {
        return "tracer";
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.spot(target, Math.round(SPOT_SECONDS * strength * TickRate.TICKS_PER_SECOND));
    }
}
