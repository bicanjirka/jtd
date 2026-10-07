package td.zone;

import td.enemy.EnemyMob;

/** What a mine does when the first enemy steps on it. */
public interface ZoneTrigger {

    void triggered(Zone mine, EnemyMob by);
}
