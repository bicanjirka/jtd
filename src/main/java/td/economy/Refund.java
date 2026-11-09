package td.economy;

/**
 * What selling or downgrading pays back, in two parts: all of what no wave has used yet, and the sell share of what
 * one has.
 *
 * @param inFull what is paid back of the unused part: all of it
 * @param shared what is paid back of the used part: its share
 */
public record Refund(int inFull, int shared) {

    private static final Refund NONE = new Refund(0, 0);

    public static Refund none() {
        return NONE;
    }

    /** All of {@code unusedPaid}, and the sell share of {@code usedPaid}. */
    public static Refund of(int unusedPaid, int usedPaid) {
        return new Refund(unusedPaid, SellRefund.of(usedPaid));
    }

    /** What this pays back beyond {@code kept}: what giving up the difference between the two is worth. */
    public Refund minus(Refund kept) {
        return new Refund(this.inFull - kept.inFull, this.shared - kept.shared);
    }

    public int total() {
        return this.inFull + this.shared;
    }

    /** Whether it gives back all that was paid: something, and none of it a share. */
    public boolean isFull() {
        return this.inFull > 0 && this.shared == 0;
    }
}
