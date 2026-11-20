package td.ui;

import java.util.OptionalInt;

/**
 * Where a magazine's row puts its slots, and which slot a point is on. Pure.
 *
 * @param slot how wide and tall a slot is
 * @param gap  the space between two slots
 */
record SlotRowLayout(int slot, int gap) {

    /** The left edge of slot {@code index}. */
    int left(int index) {
        return index * (this.slot + this.gap);
    }

    /** The slot under {@code (x, y)}, relative to the row's top-left, among {@code count}; empty between slots. */
    OptionalInt slotAt(int x, int y, int count) {
        if (x < 0 || y < 0 || y >= this.slot) {
            return OptionalInt.empty();
        }
        int index = x / (this.slot + this.gap);
        return index < count && x - this.left(index) < this.slot ? OptionalInt.of(index) : OptionalInt.empty();
    }
}
