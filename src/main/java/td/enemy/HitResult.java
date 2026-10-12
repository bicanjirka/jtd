package td.enemy;

import td.damage.Damage;

/**
 * What a hit did: the health it took, and what a shield absorbed before it got there.
 *
 * @param landed   the damage that reached health, capped at what was left
 * @param absorbed the units a shield took
 */
public record HitResult(Damage landed, int absorbed) {

    private static final HitResult NONE = new HitResult(Damage.none(), 0);

    public static HitResult none() {
        return NONE;
    }

    /** What the attacker is credited with: health taken plus what a shield absorbed. */
    public int credited() {
        return this.landed.amount() + this.absorbed;
    }

    public boolean critical() {
        return this.landed.critical();
    }
}
