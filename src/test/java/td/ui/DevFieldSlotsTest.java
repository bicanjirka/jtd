package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.DevField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class DevFieldSlotsTest {

    private static final DevField SECS = new DevField("Secs", 5f, 1f, 10f);
    private static final DevField STACKS = new DevField("Stacks", 3f, 1f, 3f);
    private static final Map<String, List<DevField>> FIELDS = Map.of("timed", List.of(SECS),
            "stacking", List.of(SECS, STACKS));

    private final DevFieldSlots<String> slots = new DevFieldSlots<>(List.of("timed", "stacking"), FIELDS::get, "timed");

    @Test
    void theSlotsTakeOnTheChosenItemsLabelsAndStartingValues() {
        this.slots.show("stacking");

        assertThat(this.slots.labels()).containsExactly("Secs", "Stacks");
        assertThat(this.slots.values(problem -> {
        })).contains(List.of(5f, 3f));
    }

    @Test
    void whatWasTypedForAnItemComesBackWhenItIsChosenAgain() {
        this.slots.textFields().getFirst().setText("2.5");

        this.slots.show("stacking");
        this.slots.show("timed");

        assertThat(this.slots.values(problem -> {
        })).contains(List.of(2.5f));
    }

    @Test
    void aSlotThatIsNoNumberIsReportedAndGivesNoValues() {
        List<String> problems = new ArrayList<>();
        this.slots.textFields().getFirst().setText("soon");

        boolean any = this.slots.values(problems::add).isPresent();

        assertThat(any).isFalse();
        assertThat(problems).containsExactly("Not a number: soon");
    }

    @Test
    void anUnusedSlotKeepsItsRoomSoTheRowDoesNotMove() {
        int withOne = this.slots.component().getPreferredSize().width;

        this.slots.show("stacking");

        assertThat(this.slots.component().getPreferredSize().width).isEqualTo(withOne);
    }
}
