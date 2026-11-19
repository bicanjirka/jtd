package td.effect;

import td.damage.AttackOrigin;

/**
 * One source's part of a stat debuff kept in shares: the stacks it is worth while it lasts.
 *
 * @param source         who keeps it going: a recast from the same source refreshes it
 * @param stacks         the stacks it is worth
 * @param remainingTicks the ticks it has left
 */
public record StackShare(AttackOrigin source, int stacks, int remainingTicks) implements SourceShare<StackShare> {

    @Override
    public StackShare withSource(AttackOrigin source) {
        return new StackShare(source, this.stacks, this.remainingTicks);
    }

    @Override
    public StackShare withRemainingTicks(int ticks) {
        return new StackShare(this.source, this.stacks, ticks);
    }

    /** The larger worth and the later end. */
    @Override
    public StackShare refreshedBy(StackShare incoming) {
        return new StackShare(this.source, Math.max(this.stacks, incoming.stacks),
                Math.max(this.remainingTicks, incoming.remainingTicks));
    }

    @Override
    public boolean isGone() {
        return this.remainingTicks <= 0;
    }
}
