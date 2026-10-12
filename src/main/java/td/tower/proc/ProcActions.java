package td.tower.proc;

import td.effect.EffectKind;
import td.enemy.EnemyMob;
import td.tower.TowerStatus;

/** What a proc's payload may make a tower do. */
public interface ProcActions {

    void applyStacks(EnemyMob target, EffectKind kind, int stacks);

    void charge(EnemyMob target, int ticks);

    void grantStatus(TowerStatus status);
}
