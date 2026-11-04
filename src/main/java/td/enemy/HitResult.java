package td.enemy;

import td.damage.Damage;

/**
 * What a hit did: the health it took, and what a shield absorbed before it got there.
 *
 * @param landed   the damage that reached health, capped at what was left
 * @param absorbed the units a shield took
 * @param dodged   whether the enemy dodged it, so nothing else about it happened
 * @param overkill the units past the health that was left, which went nowhere
 */
public record HitResult(Damage landed, int absorbed, boolean dodged, int overkill) {

    private static final HitResult NONE = new HitResult(Damage.none(), 0, false, 0);
    private static final HitResult DODGED = new HitResult(Damage.none(), 0, true, 0);

    public HitResult(Damage landed, int absorbed) {
        this(landed, absorbed, false, 0);
    }

    public static HitResult none() {
        return NONE;
    }

    public static HitResult dodge() {
        return DODGED;
    }

    public HitResult withOverkill(int overkill) {
        return new HitResult(this.landed, this.absorbed, this.dodged, overkill);
    }

    /** What the attacker is credited with: health taken plus what a shield absorbed. */
    public int credited() {
        return this.landed.amount() + this.absorbed;
    }

    public boolean critical() {
        return this.landed.critical();
    }
}
