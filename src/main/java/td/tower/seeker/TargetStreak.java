package td.tower.seeker;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;

/** How many of the Seeker's impacts in a row landed on one enemy. */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class TargetStreak {

    private EnemyMob target;
    private int hits;

    /** How many impacts in a row the current streak has; 0 once its enemy is dead. */
    public int current() {
        return this.target == null || this.target.isDead() ? 0 : this.hits;
    }

    /** How many impacts in a row landed on {@code enemy}; 0 for any other enemy. */
    public int on(EnemyMob enemy) {
        return enemy == this.target ? this.hits : 0;
    }

    /** Counts one impact on {@code enemy}: one more in a row on the same one, otherwise a new streak of one. */
    public void landed(EnemyMob enemy) {
        if (enemy == this.target) {
            this.hits++;
        } else {
            this.target = enemy;
            this.hits = 1;
        }
    }
}
