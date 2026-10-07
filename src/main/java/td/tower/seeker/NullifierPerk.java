package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;

/** Nullifier: a missile hits a shielded enemy half again as hard, then strips its shield and heal. */
public final class NullifierPerk implements SeekerPerk {

    private static final float VERSUS_SHIELD = 1.5f;

    @Override
    public float damageFactor(EnemyMob target) {
        return target.hasEffect(EffectKind.SHIELD) ? VERSUS_SHIELD : 1f;
    }

    @Override
    public void react(Impact impact, SeekerActions actions) {
        actions.dispel(impact.target());
    }
}
