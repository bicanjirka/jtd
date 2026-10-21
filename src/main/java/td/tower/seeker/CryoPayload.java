package td.tower.seeker;

import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.tower.TowerTerm;

/** Cryo: the missile freezes, as a plain one does. */
public final class CryoPayload implements Payload {

    @Override
    public MissileLook look() {
        return MissileLook.CRYO;
    }

    @Override
    public TowerTerm term() {
        return TowerTerm.CRYO_MISSILE;
    }

    @Override
    public String label() {
        return "cryo";
    }

    @Override
    public boolean freezes() {
        return true;
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.freeze(target, strength);
    }

    @Override
    public String describe() {
        return "Cryo: freezes its target";
    }
}
