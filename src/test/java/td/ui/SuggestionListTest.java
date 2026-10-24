package td.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import td.wave.WaveScriptSuggestions.Candidate;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EventDispatchThreadExtension.class)
class SuggestionListTest {

    /** A usable buff, two greyed-out buffs with different reasons, then a usable debuff. */
    private static final List<Candidate> ROWS = List.of(Candidate.token("Haste", "buff"),
            Candidate.token("Arc", "comes with an upgrade"), Candidate.token("Focus", "the tower keeps it itself"),
            Candidate.token("Jammed", "debuff"));
    private static final Set<Integer> GREYED = Set.of(1, 2);

    private final SuggestionList list = new SuggestionList(index -> Optional.empty(), index -> !GREYED.contains(index), () -> {
    });

    @Test
    void greyedOutRowsAreRuledApartOnlyFromRowsThatCanBeTakenWhateverTheirReasons() {
        this.list.show(ROWS);

        assertThat(List.of(this.list.startsGroup(1), this.list.startsGroup(2), this.list.startsGroup(3)))
                .containsExactly(true, false, true);
    }
}
