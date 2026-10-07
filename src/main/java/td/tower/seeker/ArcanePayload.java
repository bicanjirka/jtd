package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileLook;

/** Arcane: the missile Unravels its target instead of freezing it; a stronger one leaves two stacks. */
public final class ArcanePayload implements Payload {

    @Override
    public MissileLook look() {
        return MissileLook.ARCANE;
    }

    @Override
    public String label() {
        return "arcane";
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.applyStacks(target, EffectKind.UNRAVELED, (int) Math.ceil(strength));
    }
}
