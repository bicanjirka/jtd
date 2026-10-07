package td.tower.seeker;

import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.util.TickRate;

/** EMP: the missile strips its target's shield and heal and Silences it for two seconds. */
public final class EmpPayload implements Payload {

    private static final float SILENCE_SECONDS = 2f;

    @Override
    public MissileLook look() {
        return MissileLook.EMP;
    }

    @Override
    public String label() {
        return "emp";
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.dispel(target);
        actions.silence(target, Math.round(SILENCE_SECONDS * strength * TickRate.TICKS_PER_SECOND));
    }
}
