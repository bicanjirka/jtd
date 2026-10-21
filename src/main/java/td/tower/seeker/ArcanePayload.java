package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Arcane: the missile Unravels its target instead of freezing it; a stronger one leaves two stacks. */
public final class ArcanePayload implements Payload {

    @Override
    public MissileLook look() {
        return MissileLook.ARCANE;
    }

    @Override
    public TowerTerm term() {
        return TowerTerm.ARCANE_MISSILE;
    }

    @Override
    public Set<EffectKind> applies() {
        return Set.of(EffectKind.UNRAVELED);
    }

    @Override
    public String label() {
        return "arcane";
    }

    @Override
    public void deliver(EnemyMob target, float strength, PayloadActions actions) {
        actions.applyStacks(target, EffectKind.UNRAVELED, (int) Math.ceil(strength));
    }

    @Override
    public TermText effect() {
        return TermText.of("applies ").plus(EffectKind.UNRAVELED).plus(" to its target instead of ")
                .plus(EffectKind.FREEZE);
    }
}
