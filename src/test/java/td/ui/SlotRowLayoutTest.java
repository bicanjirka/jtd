package td.ui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlotRowLayoutTest {

    private final SlotRowLayout layout = new SlotRowLayout(20, 4);

    @Test
    void aPointOnASlotFindsThatSlot() {
        assertThat(this.layout.slotAt(2 * 24 + 5, 10, 10)).hasValue(2);
    }

    @Test
    void aPointInTheGapBetweenSlotsOrPastTheLastFindsNone() {
        assertThat(this.layout.slotAt(22, 10, 10)).isEmpty();
        assertThat(this.layout.slotAt(3 * 24 + 5, 10, 3)).isEmpty();
        assertThat(this.layout.slotAt(5, 20, 10)).isEmpty();
    }
}
