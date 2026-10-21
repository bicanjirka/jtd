package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

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
    public Set<EffectKind> applies() {
        return Set.of(EffectKind.FREEZE);
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
    public TermText effect() {
        return TermText.of("applies ").plus(EffectKind.FREEZE).plus(" to its target");
    }
}
