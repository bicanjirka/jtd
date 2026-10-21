package td.tower;

import td.term.Side;
import td.term.Term;
import td.term.TermText;

import java.util.OptionalInt;

/**
 * One buff or debuff a tower carries now, as its pane shows it.
 *
 * @param stacks         a count to show; 0 for none
 * @param remainingTicks ticks left on its clock; empty for one without a clock
 * @param clockTicks     what that clock started from
 * @param detail         what it does now, its numbers from the thing that grants it
 */
public record TowerBuffState(Term term, Side side, int stacks, OptionalInt remainingTicks, int clockTicks,
                             TermText detail) {

    public static TowerBuffState of(TowerTerm term, TermText detail) {
        return new TowerBuffState(term, term.side(), 0, OptionalInt.empty(), 0, detail);
    }

    public TowerBuffState withStacks(int stacks) {
        return new TowerBuffState(this.term, this.side, stacks, this.remainingTicks, this.clockTicks, this.detail);
    }

    public TowerBuffState withClock(int remainingTicks, int clockTicks) {
        return new TowerBuffState(this.term, this.side, this.stacks, OptionalInt.of(remainingTicks), clockTicks,
                this.detail);
    }
}
