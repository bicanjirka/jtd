package td.tower.seeker;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.projectile.MissileLook;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

import java.util.Set;

/** EMP: the missile strips its target's shield and heal and Silences it for two seconds. */
public final class EmpPayload implements Payload {

    private static final float SILENCE_SECONDS = 2f;

    @Override
    public MissileLook look() {
        return MissileLook.EMP;
    }

    @Override
    public TowerTerm term() {
        return TowerTerm.EMP_MISSILE;
    }

    @Override
    public Set<EffectKind> applies() {
        return Set.of(EffectKind.SILENCED);
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

    @Override
    public TermText effect() {
        return TermText.of("removes ").plus(EffectKind.SHIELD).plus(" and ").plus(EffectKind.HEAL)
                .plus(" from its target and applies ").plus(EffectKind.SILENCED)
                .plus(" for " + TermNumbers.seconds(SILENCE_SECONDS));
    }
}
