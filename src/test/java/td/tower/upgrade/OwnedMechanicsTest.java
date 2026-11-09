package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.term.TermText;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OwnedMechanicsTest {

    private static final UpgradeNode FIRST = UpgradeNode.of("first", UpgradeSlot.HEAD, "First", 10);
    private static final UpgradeNode SECOND = UpgradeNode.of("second", UpgradeSlot.HEAD, "Second", 10);
    private static final MechanicCatalogue<Mechanic> CATALOGUE = MechanicCatalogue.empty()
            .with(FIRST.id(), () -> () -> TermText.of("first"))
            .with(SECOND.id(), () -> () -> TermText.of("second"));

    @Test
    void removingANodeDropsOnlyItsMechanicsAndKeepsTheOthersInstances() {
        OwnedMechanics<Mechanic> owned = new OwnedMechanics<>(CATALOGUE);
        owned.add(FIRST);
        owned.add(SECOND);
        Mechanic kept = owned.all().get(1);

        owned.remove(List.of(FIRST));

        assertThat(owned.all()).containsExactly(kept);
    }

    @Test
    void renewingReplacesEveryMechanicWithANewOneInTheSameOrder() {
        MechanicCatalogue<Mechanic> fresh = MechanicCatalogue.empty()
                .with(FIRST.id(), () -> new Named("first"))
                .with(SECOND.id(), () -> new Named("second"));
        OwnedMechanics<Mechanic> owned = new OwnedMechanics<>(fresh);
        owned.add(FIRST);
        owned.add(SECOND);
        List<Mechanic> before = owned.all();

        owned.renew();

        assertThat(owned.all()).hasSize(2).doesNotContainAnyElementsOf(before);
        assertThat(owned.all().stream().map(mechanic -> mechanic.describe().plain()).toList())
                .containsExactly("first", "second");
    }

    /** A mechanic with a state of its own, so two of them are two. */
    private static final class Named implements Mechanic {

        private final String name;

        Named(String name) {
            this.name = name;
        }

        @Override
        public TermText describe() {
            return TermText.of(this.name);
        }
    }
}
