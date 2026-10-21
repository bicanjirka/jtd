package td.tower.seeker;

import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.tower.TowerTerm;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

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
    public String label() {
        return "tracer";
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.spot(target, Math.round(SPOT_SECONDS * strength * TickRate.TICKS_PER_SECOND));
    }

    @Override
    public String describe() {
        return "Tracer: reveals and Marks " + PerkText.seconds(Math.round(SPOT_SECONDS * TickRate.TICKS_PER_SECOND));
    }
}
