package td.effect;

import td.damage.AttackOrigin;

/**
 * One source's part of a heal: the regeneration it adds every tick while it lasts.
 *
 * @param source         who keeps it going: a recast from the same source refreshes it
 * @param perTick        the regeneration it adds, in damage units a tick
 * @param remainingTicks the ticks it has left
 */
public record HealShare(AttackOrigin source, int perTick, int remainingTicks) implements SourceShare<HealShare> {

    @Override
    public HealShare withSource(AttackOrigin source) {
        return new HealShare(source, this.perTick, this.remainingTicks);
    }

    @Override
    public HealShare withRemainingTicks(int ticks) {
        return new HealShare(this.source, this.perTick, ticks);
    }

    /** The larger amount and the later end. */
    @Override
    public HealShare refreshedBy(HealShare incoming) {
        return new HealShare(this.source, Math.max(this.perTick, incoming.perTick),
                Math.max(this.remainingTicks, incoming.remainingTicks));
    }

    @Override
    public boolean isGone() {
        return this.remainingTicks <= 0;
    }
}
