package td.tower.proc;

import td.effect.StackingDebuff;
import td.enemy.EnemyMob;
import td.tower.TowerStatus;

/** What a proc's payload may make a tower do. */
public interface ProcActions {

    void applyStacks(EnemyMob target, StackingDebuff debuff, int stacks);

    void charge(EnemyMob target, int ticks);

    void grantStatus(TowerStatus status);
}
