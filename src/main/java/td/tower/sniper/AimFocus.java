package td.tower.sniper;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;

/**
 * Who the Sniper has been shooting at and for how long: each shot at the same enemy builds on the
 * last, and aiming at another starts over.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AimFocus {

    private EnemyMob target;
    private int stacks;

    /**
     * Locks onto {@code next} for one shot and returns what that shot is fired under; the shot after
     * it builds on this one, up to {@code stackCap}. Aiming at another enemy resets the stacks, but
     * not when {@code survivesKill} and the last target died.
     */
    public AimLock lock(EnemyMob next, int stackCap, boolean survivesKill) {
        boolean fresh = next != this.target;
        if (fresh) {
            boolean carried = survivesKill && this.target != null && this.target.isDead();
            if (!carried) {
                this.stacks = 0;
            }
            this.target = next;
        }
        AimLock lock = new AimLock(Math.min(stackCap, this.stacks), fresh);
        this.stacks = Math.min(stackCap, this.stacks + 1);
        return lock;
    }
}
