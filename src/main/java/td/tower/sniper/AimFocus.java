package td.tower.sniper;

import td.enemy.EnemyMob;
import td.util.ThreadConfined;
import td.util.TickRate;

/**
 * The Sniper's rhythm: each shot builds on the last, whichever enemy it is fired at, and a stack
 * lasts {@link #STACK_SECONDS} after the last shot before it fades, one at a time. It remembers
 * whom the last shot was at so a new target can be told apart.
 */
@ThreadConfined(value = ThreadConfined.Owner.GAME_LOOP)
public final class AimFocus {

    public static final float STACK_SECONDS = 3f;
    private static final int STACK_TICKS = Math.round(STACK_SECONDS * TickRate.TICKS_PER_SECOND);

    private EnemyMob target;
    private int stacks;
    private int ticksSinceShot;

    /** One tick passes: each {@link #STACK_SECONDS} without a shot takes one stack away. */
    public void advance() {
        if (this.stacks == 0) {
            return;
        }
        this.ticksSinceShot++;
        if (this.ticksSinceShot >= STACK_TICKS) {
            this.stacks--;
            this.ticksSinceShot = 0;
        }
    }

    /**
     * Locks onto {@code next} for one shot and returns what that shot is fired under; the shot after
     * it builds on this one, up to {@code stackCap}.
     */
    public AimLock lock(EnemyMob next, int stackCap) {
        boolean fresh = next != this.target;
        this.target = next;
        AimLock lock = new AimLock(Math.min(stackCap, this.stacks), fresh);
        this.stacks = Math.min(stackCap, this.stacks + 1);
        this.ticksSinceShot = 0;
        return lock;
    }

    /** The stacks the next shot is fired under, before its cap. */
    public int stacks() {
        return this.stacks;
    }
}
