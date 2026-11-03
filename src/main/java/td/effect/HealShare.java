package td.effect;

import td.damage.AttackOrigin;

/**
 * One source's part of a heal: the regeneration it adds every tick while it lasts.
 *
 * @param source         who keeps it going: a recast from the same source refreshes it
 * @param perTick        the regeneration it adds, in damage units a tick
 * @param remainingTicks the ticks it has left
 */
public record HealShare(AttackOrigin source, int perTick, int remainingTicks) {

    /** This share as {@code incoming}, the same source's recast, leaves it: the larger amount and the later end. */
    HealShare refreshedBy(HealShare incoming) {
        return new HealShare(this.source, Math.max(this.perTick, incoming.perTick),
                Math.max(this.remainingTicks, incoming.remainingTicks));
    }

    /** A tick later; a share with no time left is gone. */
    HealShare ticked() {
        return new HealShare(this.source, this.perTick, this.remainingTicks - 1);
    }

    boolean isOver() {
        return this.remainingTicks <= 0;
    }
}
